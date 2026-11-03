import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

/**
 * Порт коллекции `metric_definitions` оригинала: пороги/описания/тексты поправок,
 * редактируемые без выкладки клиента. Источник по умолчанию — packages/shared metrics.json.
 */
@Schema({ collection: 'metric_definitions', timestamps: true })
export class MetricDefinitionDoc {
  @Prop({ required: true, unique: true, index: true })
  code!: string;

  /** Полное определение метрики из @tennis-ai/shared (MetricDefinition). */
  @Prop({ type: Object, required: true })
  definition!: Record<string, unknown>;
}

export type MetricDefinitionDocument = HydratedDocument<MetricDefinitionDoc>;
export const MetricDefinitionSchema = SchemaFactory.createForClass(MetricDefinitionDoc);
