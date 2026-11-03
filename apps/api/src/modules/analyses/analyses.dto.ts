import {
  IsArray,
  IsEnum,
  IsInt,
  IsNumber,
  IsObject,
  IsOptional,
  IsString,
  Max,
  Min,
  MinLength,
} from 'class-validator';
import type {
  AnalysisResultsRequest,
  AttachAnnotatedRequest,
  CreateAnalysisRequest,
  DetectedPhase,
  DetectStrokeRequest,
  MetricEvaluation,
} from '@tennis-ai/shared';

export class CreateAnalysisDto implements CreateAnalysisRequest {
  @IsEnum(['serve', 'forehand', 'backhand'])
  strokeType!: 'serve' | 'forehand' | 'backhand';

  @IsString()
  @MinLength(5)
  videoKey!: string;

  @IsOptional() @IsNumber() @Min(0)
  trimStartSec?: number;

  @IsOptional() @IsNumber() @Min(0)
  trimEndSec?: number;
}

export class AnalysisResultsDto implements AnalysisResultsRequest {
  @IsInt() @Min(1) @Max(240)
  fps!: number;

  @IsInt() @Min(1) @Max(2000)
  frameCount!: number;

  @IsArray()
  phases!: DetectedPhase[];

  @IsArray()
  metrics!: MetricEvaluation[];

  @IsObject()
  overall!: { score: number | null; greens: number; reds: number; unknown: number };

  @IsArray()
  topWeaknesses!: string[];

  @IsOptional() @IsArray()
  notes?: string[];
}

export class AttachAnnotatedDto implements AttachAnnotatedRequest {
  @IsString() @MinLength(5)
  annotatedKey!: string;
}

export class DetectStrokeDto implements DetectStrokeRequest {
  @IsString() @MinLength(5)
  frameKey!: string;
}
