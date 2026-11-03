import { Logger } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import {
  OnGatewayConnection,
  WebSocketGateway,
  WebSocketServer,
} from '@nestjs/websockets';
import { Env } from '../config/env';
import type { Socket } from 'socket.io';
import type { Server } from 'socket.io';

/** WS-канал прогресса/результатов: авторизация по JWT в handshake.auth.token, комнаты на пользователя. */
@WebSocketGateway({ path: '/ws', cors: { origin: '*' } })
export class EventsGateway implements OnGatewayConnection {
  @WebSocketServer()
  server!: Server;

  private readonly logger = new Logger(EventsGateway.name);

  constructor(private readonly jwt: JwtService) {}

  handleConnection(client: Socket): void {
    const token: string | undefined = client.handshake.auth?.token ?? client.handshake.query?.token;
    if (!token) {
      client.disconnect(true);
      return;
    }
    try {
      const payload = this.jwt.verify(token.replace(/^Bearer\s+/i, ''), { secret: Env.jwtSecret() }) as {
        sub: string;
      };
      void client.join(`user:${payload.sub}`);
    } catch {
      client.disconnect(true);
    }
  }

  notifyUser(userId: string, event: string, payload: unknown): void {
    if (this.server) {
      this.server.to(`user:${userId}`).emit(event, payload);
    } else {
      this.logger.warn(`WS server not ready; dropped event ${event} for ${userId}`);
    }
  }
}
