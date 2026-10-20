import crypto from 'crypto';
import { query } from '../config/database';
import { env } from '../config/env';

export interface WebhookJob {
  subscriptionId: string;
  targetUrl: string;
  secret: string;
  eventName: string;
  payload: any;
  customHeaders?: Record<string, string>;
  attemptNumber: number;
}

export class WebhookDispatcher {
  private static retryQueue: WebhookJob[] = [];
  private static isProcessing = false;

  /**
   * Dispatches an event to all active matching webhook subscriptions.
   */
  public static async dispatch(eventName: string, payload: any): Promise<void> {
    try {
      const subsResult = await query(
        `SELECT id, name, target_url, secret, headers 
         FROM webhook_subscriptions 
         WHERE is_active = TRUE AND $1 = ANY(events)`,
        [eventName]
      );

      for (const sub of subsResult.rows) {
        const job: WebhookJob = {
          subscriptionId: sub.id,
          targetUrl: sub.target_url,
          secret: sub.secret || env.WEBHOOK_DEFAULT_SECRET,
          eventName,
          payload,
          customHeaders: sub.headers || {},
          attemptNumber: 1,
        };

        // Fire and deliver asynchronously
        this.deliver(job).catch((err) => {
          console.error(`[WebhookDispatcher] Failed delivery for sub ${sub.id}:`, err);
        });
      }
    } catch (error) {
      console.error('[WebhookDispatcher] Error querying webhook subscriptions:', error);
    }
  }

  /**
   * Generates HMAC-SHA256 signature for the given payload and secret.
   */
  public static generateSignature(payload: string, secret: string, timestamp: string): string {
    const signaturePayload = `${timestamp}.${payload}`;
    return crypto.createHmac('sha256', secret).update(signaturePayload).digest('hex');
  }

  /**
   * Executes HTTP delivery with HMAC-SHA256 signature header, logs outcome,
   * and enqueues retry on failure.
   */
  public static async deliver(job: WebhookJob): Promise<boolean> {
    const timestamp = new Date().toISOString();
    const payloadString = JSON.stringify(job.payload);
    const signature = this.generateSignature(payloadString, job.secret, timestamp);

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'User-Agent': 'ENOL-Salesforze-Webhook/1.0',
      'X-Webhook-Event': job.eventName,
      'X-Webhook-Timestamp': timestamp,
      'X-Webhook-Signature': `sha256=${signature}`,
      'X-Webhook-Attempt': String(job.attemptNumber),
      ...(job.customHeaders || {}),
    };

    let responseStatus: number | null = null;
    let responseBody: string | null = null;
    let errorMessage: string | null = null;
    let isSuccess = false;

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), env.WEBHOOK_TIMEOUT_MS);

      const response = await fetch(job.targetUrl, {
        method: 'POST',
        headers,
        body: payloadString,
        signal: controller.signal,
      });

      clearTimeout(timeoutId);
      responseStatus = response.status;
      const text = await response.text();
      responseBody = text.slice(0, 2000); // Store up to 2KB response preview

      if (response.ok) {
        isSuccess = true;
      } else {
        errorMessage = `HTTP error ${response.status}: ${response.statusText}`;
      }
    } catch (err: any) {
      errorMessage = err.name === 'AbortError' ? 'Webhook delivery timed out' : err.message;
    }

    // Record delivery attempt in database
    try {
      await query(
        `INSERT INTO webhook_delivery_logs (
           subscription_id, event_name, payload, response_status,
           response_body, attempt_number, error_message, delivered_at
         ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
        [
          job.subscriptionId,
          job.eventName,
          job.payload,
          responseStatus,
          responseBody,
          job.attemptNumber,
          errorMessage,
          isSuccess ? new Date().toISOString() : null,
        ]
      );
    } catch (logErr) {
      console.error('[WebhookDispatcher] Error saving delivery log:', logErr);
    }

    // Auto-retry queue with exponential backoff if failed and retries remain
    if (!isSuccess && job.attemptNumber < env.WEBHOOK_MAX_RETRIES) {
      const nextAttempt = job.attemptNumber + 1;
      const backoffDelayMs = Math.pow(2, job.attemptNumber) * 1000; // 2s, 4s, 8s...

      setTimeout(() => {
        job.attemptNumber = nextAttempt;
        this.deliver(job).catch((retryErr) => {
          console.error(`[WebhookDispatcher] Retry ${nextAttempt} failed:`, retryErr);
        });
      }, backoffDelayMs);
    }

    return isSuccess;
  }
}
