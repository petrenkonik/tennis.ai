import { Body, Controller, Post } from '@nestjs/common';
import { Type } from 'class-transformer';
import { IsArray, IsOptional, IsString, MaxLength, MinLength, ValidateNested } from 'class-validator';
import { CurrentUser, type JwtPayload } from '../../common/current-user.decorator';
import { CoachService } from './coach.service';

class HistoryTurnDto {
  @IsString() role!: 'user' | 'assistant';
  @IsString() @MaxLength(4000) content!: string;
}

class CoachChatDto {
  @IsString() @MinLength(1) @MaxLength(4000)
  message!: string;

  @IsOptional() @IsString()
  analysisId?: string;

  @IsOptional() @IsArray()
  @ValidateNested({ each: true })
  @Type(() => HistoryTurnDto)
  history?: HistoryTurnDto[];
}

@Controller('coach')
export class CoachController {
  constructor(private readonly coach: CoachService) {}

  @Post('chat')
  chat(@CurrentUser() u: JwtPayload, @Body() dto: CoachChatDto) {
    return this.coach.chat(u, dto.message, dto.analysisId, dto.history ?? []);
  }
}
