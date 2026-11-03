import { BadRequestException, Body, Controller, Get, Injectable, Module, Post } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { MongooseModule } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { IsString, MinLength } from 'class-validator';
import { CurrentUser, type JwtPayload } from '../../common/current-user.decorator';
import { Analysis, AnalysisDocument, AnalysisSchema } from '../../common/schemas/analysis.schema';
import { Plan, PlanDocument, PlanSchema } from '../../common/schemas/plan.schema';
import { DrillsModule } from '../drills/drills.module';
import { DrillsService } from '../drills/drills.service';

class CreatePlanDto {
  @IsString() @MinLength(5)
  analysisId!: string;
}

@Controller('plans')
export class PlansController {
  constructor(private readonly plans: PlansService) {}

  @Get()
  list(@CurrentUser() u: JwtPayload) {
    return this.plans.listForUser(u.sub);
  }

  @Get('active')
  active(@CurrentUser() u: JwtPayload) {
    return this.plans.activeForUser(u.sub);
  }

  /** POST /api/plans — собрать план по weaknesses анализа (порт trainingPlanAgent, серверная v0-логика). */
  @Post()
  create(@CurrentUser() u: JwtPayload, @Body() dto: CreatePlanDto) {
    return this.plans.createFromAnalysis(u.sub, dto.analysisId);
  }
}

@Injectable()
export class PlansService {
  constructor(
    @InjectModel(Plan.name) private readonly plans: Model<PlanDocument>,
    @InjectModel(Analysis.name) private readonly analyses: Model<AnalysisDocument>,
    private readonly drills: DrillsService,
  ) {}

  async listForUser(userId: string): Promise<PlanDocument[]> {
    return this.plans.find({ userId }).sort({ createdAt: -1 });
  }

  async activeForUser(userId: string): Promise<PlanDocument | null> {
    return this.plans.findOne({ userId, status: 'active' }).sort({ createdAt: -1 });
  }

  async createFromAnalysis(userId: string, analysisId: string): Promise<PlanDocument> {
    if (!Types.ObjectId.isValid(analysisId)) throw new BadRequestException('bad analysisId');
    const analysis = await this.analyses.findById(analysisId);
    if (!analysis || analysis.userId !== userId) throw new BadRequestException('analysis not found');

    const weaknesses: string[] = analysis.topWeaknesses ?? [];
    const pool = await this.drills.recommended(userId, analysisId);
    // v0: одна неделя на каждое слабое место (максимум 4), drills матчатся по targetMetrics.
    const weeks = weaknesses.slice(0, 4).map((metric) => ({
      focus: metric,
      drillSlugs: pool.filter((d) => d.targetMetrics.includes(metric)).map((d) => d.slug).slice(0, 3),
      sessionsPerWeek: 3,
    }));

    await this.plans.updateMany({ userId, status: 'active' }, { $set: { status: 'archived' } });
    return this.plans.create({
      userId,
      analysisId,
      title: 'Based on your analysis — personalized',
      description: `Targets: ${weaknesses.join(', ') || 'general technique'}`,
      weeks: weeks.length > 0 ? weeks : [{ focus: 'general', drillSlugs: pool.slice(0, 3).map((d) => d.slug), sessionsPerWeek: 3 }],
      targetMetrics: weaknesses,
      status: 'active',
    });
  }
}

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Plan.name, schema: PlanSchema },
      { name: Analysis.name, schema: AnalysisSchema },
    ]),
    DrillsModule,
  ],
  controllers: [PlansController],
  providers: [PlansService],
})
export class PlansModule {}
