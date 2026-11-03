import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

/** Период Pro-подписки (Telegram Stars не поддерживают авто-renew — период покупается явно). */
@Schema({ collection: 'subscriptions', timestamps: true })
export class Subscription {
  @Prop({ required: true, index: true })
  userId!: string;

  @Prop({ required: true })
  telegramPaymentChargeId!: string;

  @Prop({ required: true, enum: ['pro_1m', 'pro_3m', 'pro_12m'] })
  productId!: string;

  @Prop({ required: true })
  months!: number;

  @Prop({ required: true })
  stars!: number;

  @Prop({ required: true })
  periodStart!: Date;

  @Prop({ required: true, index: true })
  periodEnd!: Date;

  @Prop({ enum: ['active', 'expired', 'refunded'], default: 'active' })
  status!: string;
}

export type SubscriptionDocument = HydratedDocument<Subscription>;
export const SubscriptionSchema = SchemaFactory.createForClass(Subscription);
