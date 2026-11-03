/** Simple environment access with no external dependencies. Critical values are validated where they are used. */
export const Env = {
  port(): number {
    return Number(process.env.PORT ?? 3000);
  },
  jwtSecret(): string {
    const v = process.env.JWT_SECRET;
    if (!v || v === 'change-me') {
      // Do not break the dev startup, but log loudly.
      console.warn('[env] JWT_SECRET is not set or default — do not use in production');
      return 'dev-insecure-secret';
    }
    return v;
  },
  botToken(): string {
    return process.env.TELEGRAM_BOT_TOKEN ?? '';
  },
  botWebhookSecret(): string {
    return process.env.TELEGRAM_WEBHOOK_SECRET ?? '';
  },
  geminiKey(): string {
    return process.env.GEMINI_API_KEY ?? '';
  },
  /** Z.ai GLM: OpenAI-compatible chat completions. */
  glmKey(): string {
    return process.env.GLM_API_KEY ?? process.env.ZAI_API_KEY ?? '';
  },
  glmBaseUrl(): string {
    return process.env.GLM_BASE_URL ?? 'https://api.z.ai/api/paas/v4';
  },
  glmModel(): string {
    return process.env.GLM_MODEL ?? 'glm-5.3-flash';
  },
  /** For detect-stroke (vision). Defaults to the main model. */
  glmVisionModel(): string {
    return process.env.GLM_VISION_MODEL ?? this.glmModel();
  },
  mongoUri(): string {
    return process.env.MONGO_URI ?? 'mongodb://127.0.0.1:27017/tennis-ai';
  },
  s3() {
    return {
      endpoint: process.env.S3_ENDPOINT ?? '',
      region: process.env.S3_REGION ?? 'auto',
      bucket: process.env.S3_BUCKET ?? 'tennis-ai',
      accessKeyId: process.env.S3_ACCESS_KEY_ID ?? '',
      secretAccessKey: process.env.S3_SECRET_ACCESS_KEY ?? '',
    };
  },
};
