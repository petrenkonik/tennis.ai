import { IsIn, IsOptional, IsString, Matches } from 'class-validator';
import type { UploadKind } from '@tennis-ai/shared';

export class PresignDto {
  @IsIn(['original', 'frame', 'annotated'])
  kind!: UploadKind;

  @IsString()
  contentType!: string;

  @IsOptional()
  @Matches(/^[0-9a-fA-F-]{8,64}$/)
  sessionId?: string;
}
