import { Controller, Get, Param, Query } from '@nestjs/common';
import { IsOptional, IsString } from 'class-validator';
import { CurrentUser, type JwtPayload } from '../../common/current-user.decorator';
import { DrillsService } from './drills.service';

class DrillQuery {
  @IsOptional() @IsString()
  difficulty?: string;

  @IsOptional() @IsString()
  targetMetric?: string;
}

@Controller('drills')
export class DrillsController {
  constructor(private readonly drills: DrillsService) {}

  @Get()
  list(@Query() q: DrillQuery) {
    return this.drills.list(q);
  }

  @Get('recommended')
  recommended(@CurrentUser() u: JwtPayload, @Query('analysisId') analysisId?: string) {
    return this.drills.recommended(u.sub, analysisId);
  }

  @Get(':slug')
  bySlug(@Param('slug') slug: string) {
    return this.drills.bySlug(slug);
  }
}
