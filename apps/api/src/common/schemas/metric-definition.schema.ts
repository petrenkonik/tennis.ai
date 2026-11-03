import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

/**
 * Port of the original `metric_definitions` collection: thresholds/descriptions/correction
 * texts, editable without shipping a client release. Default source — packages/shared metrics.json.
 */
@Schema({ collection: 'metric_definitions', timestamps: true })
export class MetricDefinitionDoc {
  @Prop({ required: true, unique: true, index: true })
  code!: string;

  /** Full metric definition from @tennis-ai/shared (MetricDefinition). */
  @Prop({ type: Object, required: true })
  definition!: Record<string, unknown>;
}

export type MetricDefinitionDocument = HydratedDocument<MetricDefinitionDoc>;
export const MetricDefinitionSchema = SchemaFactory.createForClass(MetricDefinitionDoc);
