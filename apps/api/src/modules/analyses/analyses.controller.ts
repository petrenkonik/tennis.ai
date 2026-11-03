import { Body, Controller, Delete, Get, Param, Post, Query } from '@nestjs/common';
import { CurrentUser, type JwtPayload } from '../../common/current-user.decorator';
import { AnalysesService } from './analyses.service';
import { AnalysisResultsDto, AttachAnnotatedDto, CreateAnalysisDto, DetectStrokeDto } from './analyses.dto';
import { CoachService } from '../coach/coach.service';

@Controller()
export class AnalysesController {
  constructor(
    private readonly analyses: AnalysesService,
    private readonly coach: CoachService,
  ) {}

  @Post('analyses')
  create(@CurrentUser() u: JwtPayload, @Body() dto: CreateAnalysisDto) {
    return this.analyses.create(u, dto);
  }

  @Post('analyses/:id/results')
  saveResults(@CurrentUser() u: JwtPayload, @Param('id') id: string, @Body() dto: AnalysisResultsDto) {
    return this.analyses.saveResults(u, id, dto);
  }

  @Post('analyses/:id/annotated')
  attachAnnotated(@CurrentUser() u: JwtPayload, @Param('id') id: string, @Body() dto: AttachAnnotatedDto) {
    return this.analyses.attachAnnotated(u, id, dto.annotatedKey);
  }

  /** Тип удара по кадру: кадр грузится через /uploads/presign(kind: frame), API классифицирует через Gemini vision. */
  @Post('analysis/detect-stroke')
  detectStroke(@CurrentUser() u: JwtPayload, @Body() dto: DetectStrokeDto) {
    return this.coach.detectStroke(u, dto.frameKey);
  }

  @Get('analyses')
  list(@CurrentUser() u: JwtPayload, @Query('limit') limit?: string) {
    return this.analyses.list(u, limit ? Number(limit) : 50);
  }

  @Get('analyses/:id')
  get(@CurrentUser() u: JwtPayload, @Param('id') id: string) {
    return this.analyses.get(u, id);
  }

  /** Presigned URL видео сессии для плеера Results (annotated, иначе original). */
  @Get('analyses/:id/video')
  video(@CurrentUser() u: JwtPayload, @Param('id') id: string) {
    return this.analyses.videoUrl(u, id);
  }

  @Delete('analyses/:id')
  async remove(@CurrentUser() u: JwtPayload, @Param('id') id: string) {
    await this.analyses.remove(u, id);
    return { ok: true };
  }
}
