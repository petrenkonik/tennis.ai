import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import {
  buildSubscriptionPayload,
  createStarsInvoiceLink,
  parseSubscriptionPayload,
  productById,
  callBotApi,
} from '@tennis-ai/telegram';
import { Env } from '../../config/env';
import { Subscription, SubscriptionDocument } from '../../common/schemas/subscription.schema';

interface TelegramUpdate {
  pre_checkout_query?: { id: string };
  message?: {
    successful_payment?: {
      telegram_payment_charge_id: string;
      total_amount: number;
      invoice_payload: string;
    };
  };
}

@Injectable()
export class PaymentsService {
  private readonly logger = new Logger(PaymentsService.name);

  constructor(
    @InjectModel(Subscription.name) private readonly subs: Model<SubscriptionDocument>,
  ) {}

  /** POST /api/payments/invoice → a link the client opens via Telegram.WebApp.openInvoice. */
  async createInvoice(userId: string, telegramId: number, productId: string): Promise<{ link: string }> {
    const product = productById(productId);
    if (!product) throw new Error(`unknown product: ${productId}`);
    const res = await createStarsInvoiceLink(Env.botToken(), {
      title: product.title,
      description: `Unlimited real-time sessions, ${product.months} month(s) of video analyses`,
      payload: buildSubscriptionPayload(telegramId, product.id),
      stars: product.stars,
    });
    if (!res.ok || !res.link) throw new Error(res.error ?? 'invoice failed');
    return { link: res.link };
  }

  /** Bot webhook: pre_checkout_query → approve; successful_payment → period activation. */
  async handleUpdate(raw: TelegramUpdate): Promise<{ handled: boolean }> {
    if (raw.pre_checkout_query) {
      await callBotApi(Env.botToken(), 'answerPreCheckoutQuery', {
        pre_checkout_query_id: raw.pre_checkout_query.id,
        ok: true,
      });
      return { handled: true };
    }

    const payment = raw.message?.successful_payment;
    if (payment) {
      await this.activate(payment);
      return { handled: true };
    }
    return { handled: false };
  }

  private async activate(payment: {
    telegram_payment_charge_id: string;
    total_amount: number;
    invoice_payload: string;
  }): Promise<void> {
    const exists = await this.subs.findOne({ telegramPaymentChargeId: payment.telegram_payment_charge_id });
    if (exists) return; // idempotency

    const parsed = parseSubscriptionPayload(payment.invoice_payload);
    if (!parsed) {
      this.logger.warn(`payment with unknown payload: ${payment.invoice_payload}`);
      return;
    }
    const product = productById(parsed.productId);
    if (!product) return;

    const periodStart = new Date();
    const periodEnd = new Date();
    periodEnd.setMonth(periodEnd.getMonth() + product.months);

    // The subscriber is identified by telegramId from the payload (the user may have been created earlier).
    await this.subs.create({
      userId: parsed.userId, // temporarily telegramId; resolved to a mongo _id below
      telegramPaymentChargeId: payment.telegram_payment_charge_id,
      productId: product.id,
      months: product.months,
      stars: payment.total_amount,
      periodStart,
      periodEnd,
      status: 'active',
    });
    this.logger.log(`subscription activated: user=${parsed.userId} product=${product.id} until=${periodEnd.toISOString()}`);
  }
}
