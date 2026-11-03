import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

/** Training plan built from analysis weaknesses (port of training_plan_service). */
@Schema({ collection: 'plans', timestamps: true })
export class Plan {
  @Prop({ required: true, index: true })
  userId!: string;

  @Prop({ index: true })
  analysisId?: string;

  @Prop({ required: true })
  title!: string;

  @Prop()
  description?: string;

  @Prop({ type: Array, default: [] })
  weeks!: Array<{ focus: string; drillSlugs: string[]; sessionsPerWeek: number }>;

  @Prop({ type: [String], default: [] })
  targetMetrics!: string[];

  @Prop({ enum: ['active', 'completed', 'archived'], default: 'active' })
  status!: string;
}

export type PlanDocument = HydratedDocument<Plan>;
export const PlanSchema = SchemaFactory.createForClass(Plan);
