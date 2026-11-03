import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { JwtService } from '@nestjs/jwt';
import { Env } from '../config/env';
import { IS_PUBLIC_KEY } from './public.decorator';
import type { JwtPayload } from './current-user.decorator';

/** Глобальный guard: всё, кроме @Public(), требует Bearer JWT. */
@Injectable()
export class JwtAuthGuard implements CanActivate {
  constructor(
    private readonly jwt: JwtService,
    private readonly reflector: Reflector,
  ) {}

  canActivate(context: ExecutionContext): boolean {
    const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);
    if (isPublic) return true;

    const request = context.switchToHttp().getRequest();
    const header: string | undefined = request.headers?.authorization;
    if (!header?.startsWith('Bearer ')) throw new UnauthorizedException();
    try {
      request.user = this.jwt.verify(header.slice(7), { secret: Env.jwtSecret() }) as JwtPayload;
      return true;
    } catch {
      throw new UnauthorizedException();
    }
  }
}
