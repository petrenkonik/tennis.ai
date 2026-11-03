import { Body, Controller, Get, Patch } from '@nestjs/common';
import { IsIn, IsNumber, IsOptional, IsString, Max, Min } from 'class-validator';
import { CurrentUser, type JwtPayload } from '../../common/current-user.decorator';
import { UsersService } from './users.service';

class UpdateProfileDto {
  @IsOptional() @IsString()
  level?: string;

  @IsOptional() @IsIn(['left', 'right'])
  dominantHand?: 'left' | 'right';

  @IsOptional() @IsNumber() @Min(120) @Max(230)
  heightCm?: number;

  @IsOptional() @IsString()
  language?: string;
}

@Controller('users')
export class UsersController {
  constructor(private readonly users: UsersService) {}

  @Get('me')
  async me(@CurrentUser() u: JwtPayload) {
    const user = await this.users.me(u.sub);
    const quota = await this.users.quotaState(u.sub);
    return this.users.toDto(user, quota);
  }

  @Patch('me')
  async update(@CurrentUser() u: JwtPayload, @Body() dto: UpdateProfileDto) {
    const user = await this.users.updateProfile(u.sub, dto);
    const quota = await this.users.quotaState(u.sub);
    return this.users.toDto(user, quota);
  }
}
