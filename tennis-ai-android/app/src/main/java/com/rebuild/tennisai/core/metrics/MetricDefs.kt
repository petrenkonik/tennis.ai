package com.rebuild.tennisai.core.metrics

import com.rebuild.tennisai.core.model.CorrectionText
import com.rebuild.tennisai.core.model.Lang
import com.rebuild.tennisai.core.model.Moment
import com.rebuild.tennisai.core.model.StrokeType
import com.rebuild.tennisai.core.model.ValueKind

/**
 * Metric definition — a clone of the original's system: codes are 1:1, descriptions and
 * ranges are verbatim from Dart AOT v2.0.19 (reference/dart-aot-recovery/, offsets in
 * GAP_ANALYSIS.md). The computation formulas are our geometry (the formula sources
 * are not recoverable from the snapshot), the ranges are the original ones.
 *
 * ndMin/ndMax — the non-dominant side range (the original's D/ND pairs).
 */
data class MetricDef(
    val id: String,
    val kind: ValueKind,
    val proMin: Float,
    val proMax: Float,
    val ndMin: Float? = null,
    val ndMax: Float? = null,
    val fmt: String = "%.0f",
    val weight: Float = 1f,
    val nameEn: String,
    val nameRu: String,
    val descEn: String,
    val descRu: String,
    val lowEn: String, val lowRu: String,
    val highEn: String, val highRu: String,
    /** Verbatim pro-comparison string from the original (if recovered). */
    val proNote: String? = null,
) {
    fun name(lang: Lang) = if (lang == Lang.RU) nameRu else nameEn
    fun desc(lang: Lang) = if (lang == Lang.RU) descRu else descEn
    fun correction(lang: Lang, value: Float): CorrectionText {
        val low = value < proMin
        val title = (if (lang == Lang.RU) (if (low) "Мало: " else "Много: ") else (if (low) "Too low: " else "Too high: ")) + fmt.format(value) + kind.unit(lang)
        val body = if (lang == Lang.RU) (if (low) lowRu else highRu) else (if (low) lowEn else highEn)
        return CorrectionText(title, body)
    }
}

object MetricDefs {

    // ---- Common ---------------------------------------------------------------
    val KNEE_FLEXION = MetricDef(
        id = "KNEE_FLEXION", kind = ValueKind.DEGREES, proMin = 110f, proMax = 170f,
        nameEn = "Knee Flexion", nameRu = "Сгибание коленей",
        descEn = "Knee bend during loading or contact. Deep bend stores elastic energy for explosive extension.",
        descRu = "Сгиб коленей при загрузке или контакте. Глубокий сгиб запасает упругую энергию.",
        lowEn = "BEND KNEES MORE. Bend your knees more! Load the spring before you push up.",
        lowRu = "СИЛЬНЕЕ СОГНИ КОЛЕНИ. Заряди пружину перед толчком вверх.",
        highEn = "Too much knee bend. Extend slightly for balance.",
        highRu = "Слишком глубокий присед. Чуть выпрямись для баланса.",
        proNote = "(K110-180/H100-180)",
    )
    val DOM_KNEE = MetricDef(
        id = "DOM_KNEE", kind = ValueKind.DEGREES, proMin = 110f, proMax = 180f,
        nameEn = "Dominant Knee", nameRu = "Доминантное колено",
        descEn = "Knee angle of the dominant leg during the phase.",
        descRu = "Угол колена доминантной ноги в фазе.",
        lowEn = "Push your weight forward into the shot. Adjust your leg position. Bend and push.",
        lowRu = "Переноси вес в удар. Согни и вытолкнись ногой.",
        highEn = "Adjust your leg position. Bend and push.",
        highRu = "Подстрой позицию ног. Согни и вытолкнись.",
    )
    val NDOM_KNEE = MetricDef(
        id = "NDOM_KNEE", kind = ValueKind.DEGREES, proMin = 100f, proMax = 180f,
        nameEn = "Non-Dominant Knee", nameRu = "Недоминантное колено",
        descEn = "Knee angle of the non-dominant leg during the phase.",
        descRu = "Угол колена недоминантной ноги в фазе.",
        lowEn = "Adjust your leg position. Bend and push.",
        lowRu = "Подстрой позицию ног. Согни и вытолкнись.",
        highEn = "Adjust your leg position. Bend and push.",
        highRu = "Подстрой позицию ног. Согни и вытолкнись.",
    )
    val ANKLE_HIP_SHOULDER = MetricDef(
        id = "ALIGNMENT", kind = ValueKind.DEGREES, proMin = 110f, proMax = 170f,
        nameEn = "Kinetic Chain Alignment", nameRu = "Кинетическая цепь",
        descEn = "Kinetic chain alignment from ankle through hip to shoulder. (Ankle-Hip-Shoulder 110-170)",
        descRu = "Выравнивание кинетической цепи голеностоп–бедро–плечо (110–170°).",
        lowEn = "Align heel through hip to shoulder. Stay stacked.",
        lowRu = "Выровняй линию пятка–бедро–плечо. Держи вертикаль.",
        highEn = "Check the chain line — keep the ankle under the hip.",
        highRu = "Проверь линию цепи — голеностоп под бедром.",
    )
    val HEEL_HIP_SHOULDER = MetricDef(
        id = "HEEL_HIP_SHOULDER", kind = ValueKind.DEGREES, proMin = 155f, proMax = 179f,
        nameEn = "Landing Alignment", nameRu = "Осанка при приземлении",
        descEn = "Alignment from heel through hip to shoulder. Should be nearly straight. (155-179)",
        descRu = "Линия пятка–бедро–плечо при приземлении. Почти прямая (155–179°).",
        lowEn = "Land tall — straighten the heel-hip-shoulder line.",
        lowRu = "Приземляйся вытянутым — выпрями линию пятка–бедро–плечо.",
        highEn = "Slight knee bend on landing protects the joints.",
        highRu = "Лёгкий сгиб колена при приземлении защищает суставы.",
    )
    val STANCE_WIDTH = MetricDef(
        id = "STANCE_WIDTH", kind = ValueKind.RATIO, proMin = 0.8f, proMax = 1.5f, fmt = "%.2f",
        nameEn = "Stance Width", nameRu = "Ширина стойки",
        descEn = "Distance between feet relative to shoulder width. (Toes 3-15cm / Shoulder 14-72cm; 0.8-1.5x)",
        descRu = "Стопы относительно ширины плеч (0.8–1.5x).",
        lowEn = "Widen your stance. Feet should be shoulder-width apart.",
        lowRu = "Расширь стойку. Стопы на ширине плеч.",
        highEn = "Feet too wide. Narrow the stance slightly.",
        highRu = "Слишком широко. Чуть сведи стопы.",
    )
    val TRUNK_TILT = MetricDef(
        id = "TRUNK_TILT", kind = ValueKind.DEGREES, proMin = 5f, proMax = 39f,
        nameEn = "Trunk Tilt", nameRu = "Наклон корпуса",
        descEn = "Lateral tilt of the trunk. Controlled tilt helps direct the ball. (Trunk tilt 5-39 / 10-45)",
        descRu = "Боковой наклон корпуса (5–39° / 10–45°).",
        lowEn = "Lean into the court a bit more at contact.",
        lowRu = "Наклонись в корт чуть больше в контакте.",
        highEn = "Too much lean. Stay more upright at contact.",
        highRu = "Слишком сильный наклон. Держи вертикаль в контакте.",
    )
    val SHOULDERS_LEVEL = MetricDef(
        id = "SHOULDERS_LEVEL", kind = ValueKind.DEGREES, proMin = 0f, proMax = 12f,
        nameEn = "Shoulders Horizontal", nameRu = "Горизонталь плеч",
        descEn = "Horizontal alignment of both shoulders. Should be level during stance/follow-through. (Shoulders horizontal <12)",
        descRu = "Горизонтальность линии плеч (<12°).",
        lowEn = "Shoulders level is fine — keep it through the finish.",
        lowRu = "Плечи горизонтальны — держи до конца проводки.",
        highEn = "Level the shoulders in the follow-through.",
        highRu = "Выровняй плечи в проводке.",
    )
    val BODY_CENTER = MetricDef(
        id = "BODY_CENTER", kind = ValueKind.RATIO, proMin = -0.25f, proMax = 0.25f, fmt = "%.2f",
        nameEn = "Body Center Offset", nameRu = "Смещение центра",
        descEn = "Body center offset from the baseline. Should be centered and balanced.",
        descRu = "Смещение центра тела от базовой линии. Держи центр и баланс.",
        lowEn = "Adjust your body position. Stay balanced.",
        lowRu = "Подстрой положение корпуса. Держи баланс.",
        highEn = "Adjust your body position. Stay balanced.",
        highRu = "Подстрой положение корпуса. Держи баланс.",
    )

    // ---- Serve ---------------------------------------------------------------
    val M1_ARMS_POSITION = MetricDef(
        id = "M1_ARMS_POSITION", kind = ValueKind.DEGREES, proMin = 20f, proMax = 170f,
        nameEn = "Arms Position", nameRu = "Позиция рук",
        descEn = "Both arms position during the stroke. Guides racket and balances body. Ready: racket at chest height, elbows slightly away.",
        descRu = "Позиция обеих рук. Ракетка у груди, локти чуть в стороны.",
        lowEn = "Your arms are hanging too low in the ready position. Hold the racket at chest height.",
        lowRu = "Руки слишком низко в стойке. Ракетку — к груди.",
        highEn = "Adjust your arm position.",
        highRu = "Подстрой позицию рук.",
    )
    val M1_FOOT_ANGLE = MetricDef(
        id = "M1_FOOT_ANGLE", kind = ValueKind.DEGREES, proMin = 30f, proMax = 45f,
        nameEn = "Foot Angle", nameRu = "Разворот стопы",
        descEn = "Front foot angled toward the net post at 30-45 degrees.",
        descRu = "Передняя стопа развёрнута к сеточной стойке на 30–45°.",
        lowEn = "Your front foot is too parallel to the baseline. Angle it toward the net post at about 30-45.",
        lowRu = "Стопа слишком параллельна задней линии. Разверни к сеточной стойке на 30–45°.",
        highEn = "Angle your front foot toward the net post at 30-45.",
        highRu = "Разверни переднюю стопу к сеточной стойке на 30–45°.",
    )
    val M2_KNEES_POSITION = MetricDef(
        id = "M2_KNEES_POSITION", kind = ValueKind.DEGREES, proMin = 65f, proMax = 160f,
        nameEn = "Knees Position", nameRu = "Позиция коленей",
        descEn = "Knee flexion at toss & backswing. (K110-180/H100-180)",
        descRu = "Сгиб коленей на подбросе и замахе.",
        lowEn = "Extend your legs more. You are too bent at this phase.",
        lowRu = "Больше вытянись на этой фазе — слишком согнут.",
        highEn = "Bend it. Target: 65-160.",
        highRu = "Согни колени. Цель: 65–160°.",
    )
    val M2_UNIT_TURN = MetricDef(
        id = "M2_UNIT_TURN", kind = ValueKind.DEGREES, proMin = 10f, proMax = 30f,
        nameEn = "Shoulder-Hip Coil", nameRu = "Скрутка плечи–бёдра",
        descEn = "Angle between shoulder line and hip line. Creates the coil for power. (Shoulder-Hip 10-30)",
        descRu = "Угол между линиями плеч и бёдер (10–30°). Пружина для мощности.",
        lowEn = "Coil more — turn the shoulders away from the net.",
        lowRu = "Больше скрутки — разверни плечи от сетки.",
        highEn = "Core rotation: full rotation creates the coil for power.",
        highRu = "Скрутка корпуса создаёт мощность — не перекручивайся.",
    )
    val M2_WEIGHT_TRANSFER = MetricDef(
        id = "M2_WEIGHT_TRANSFER", kind = ValueKind.RATIO, proMin = 0.1f, proMax = 1.2f, fmt = "%.2f",
        nameEn = "Weight Transfer", nameRu = "Перенос веса",
        descEn = "Weight transfer pattern during loading. Back foot should push forward. Outside heel >= 0.5cm elevation.",
        descRu = "Перенос веса при загрузке. Задняя нога толкает вперёд.",
        lowEn = "Back foot push initiates weight transfer and rotation. Push your weight forward into the shot.",
        lowRu = "Толчок задней ногой запускает перенос веса. Толкай вес в удар.",
        highEn = "Weight transfer is strong — keep balance at the finish.",
        highRu = "Перенос веса сильный — держи баланс в финише.",
    )
    val M3_TRIPLE_EXTENSION = MetricDef(
        id = "M3_TRIPLE_EXTENSION", kind = ValueKind.DEGREES, proMin = 140f, proMax = 180f,
        nameEn = "Triple Extension", nameRu = "Тройное разгибание",
        descEn = "Simultaneous extension of ankle, knee, and hip. (Ankle-Knee-Hip rising)",
        descRu = "Одновременное разгибание голеностопа, колена и бедра.",
        lowEn = "Push up more! Drive from ankles, knees and hips together. Extend fully during the upward phase.",
        lowRu = "Толкайся сильнее! От голеностопа, коленей и бёдер одновременно.",
        highEn = "Extension is fine — keep timing smooth.",
        highRu = "Разгибание в порядке — держи тайминг.",
    )
    val M3_TROPHY_POSITION = MetricDef(
        id = "M3_TROPHY_POSITION", kind = ValueKind.DEGREES, proMin = 40f, proMax = 120f,
        ndMin = 79f, ndMax = 170f,
        nameEn = "Trophy Position", nameRu = "Позиция «трофи»",
        descEn = "The classic pose before the forward swing. Shoulder-elbow alignment and racket drop angle. (Arm cocked 40-120 / Non-dom 79-170)",
        descRu = "Классическая поза перед ударом: плечо–локоть и угол замаха (рука 40–120° / недом. 79–170°).",
        lowEn = "Your elbow is at shoulder height and the racket points to the sky. Adjust your arm position.",
        lowRu = "Локоть на высоте плеча, ракетка в небо. Подстрой руку.",
        highEn = "Adjust your arm position. Stay relaxed.",
        highRu = "Подстрой руку. Держи расслабленно.",
    )
    val M4_TRUNK_ROTATION = MetricDef(
        id = "M4_TRUNK_ROTATION_BODY_ALIGNMENT", kind = ValueKind.DEGREES, proMin = 5f, proMax = 45f,
        nameEn = "Trunk Rotation & Body Alignment", nameRu = "Ротация корпуса",
        descEn = "Core rotation during the forward swing. Full rotation creates the coil for power. (Trunk tilt 10-45; rotation 3-20cm)",
        descRu = "Ротация корпуса в ударе (наклон 10–45°, ротация 3–20см).",
        lowEn = "Rotate the trunk through the shot. Mapping torso rotation.",
        lowRu = "Проверни корпус в ударе.",
        highEn = "Too much lean. Stay more upright at contact.",
        highRu = "Слишком сильный наклон. Держи вертикаль в контакте.",
    )
    val M5_CONTACT_POINT_HEIGHT_DISTANCE = MetricDef(
        id = "M5_CONTACT_POINT_HEIGHT_DISTANCE", kind = ValueKind.RATIO, proMin = 1.5f, proMax = 2.3f, fmt = "%.2f",
        nameEn = "Contact Point Height", nameRu = "Высота точки контакта",
        descEn = "Contact height relative to player height (cm scale). Hit at the highest point! Full extension at contact.",
        descRu = "Высота контакта относительно роста. Бей в высшей точке — полное вытяжение.",
        lowEn = "Hit at the highest point! Full extension at contact. Extend your arm more.",
        lowRu = "Бей в высшей точке! Полное вытяжение в контакте.",
        highEn = "Contact height is excellent.",
        highRu = "Высота контакта отличная.",
        proNote = "(H:50-165cm D:10-100cm)",
    )
    val M5_RACKET_HEAD_POSITION = MetricDef(
        id = "M5_RACKET_HEAD_POSITION", kind = ValueKind.DEGREES, proMin = 0f, proMax = 25f,
        nameEn = "Racket Head Position", nameRu = "Позиция головы ракетки",
        descEn = "Racket face angle at contact. Should be parallel to shoulders, vertical. (Racket face parallel shoulders <25deg; Racket 0-30) — requires racket detection.",
        descRu = "Угол ракетки в контакте: параллельно плечам, вертикально (<25°) — требуется детекция ракетки.",
        lowEn = "Control your racket face. Keep it square at contact.",
        lowRu = "Контролируй плоскость ракетки. Держи квадратно в контакте.",
        highEn = "Racket face looks good.",
        highRu = "Плоскость ракетки в порядке.",
    )
    val TOSS_ARM_EXTENSION = MetricDef(
        id = "TOSS_ARM_EXTENSION", kind = ValueKind.DEGREES, proMin = 152f, proMax = 180f,
        nameEn = "Toss Arm Extension", nameRu = "Рука подброса",
        descEn = "Extension of the toss arm elbow. Straight arm produces consistent toss. (Toss arm 152-180)",
        descRu = "Разгибание руки подброса (152–180°).",
        lowEn = "Extend the toss arm more. Straight arm guides a consistent toss.",
        lowRu = "Выпрями подбрасывающую руку — будет стабильный подброс.",
        highEn = "Toss arm looks fine — keep it relaxed.",
        highRu = "Рука подброса в порядке.",
    )
    val TOSS_ARM_SPEED = MetricDef(
        id = "TOSS_ARM_SPEED", kind = ValueKind.SPEED_MS, proMin = 1.5f, proMax = 6.0f, fmt = "%.1f",
        nameEn = "Toss Arm Speed", nameRu = "Скорость руки подброса",
        descEn = "Speed of the toss arm during ball release. Controlled speed ensures consistent toss height. (1.5-6.0 m/s)",
        descRu = "Скорость руки при выпуске мяча (1.5–6.0 м/с).",
        lowEn = "Release the ball with a smooth, controlled lift.",
        lowRu = "Выпускай мяч плавным контролируемым движением.",
        highEn = "Too fast — smooth release keeps the toss consistent.",
        highRu = "Слишком резко — плавный выпуск стабильнее.",
    )
    val WRIST_SPEED = MetricDef(
        id = "WRIST_SPEED", kind = ValueKind.SPEED_MS, proMin = 10f, proMax = 25f, fmt = "%.1f",
        nameEn = "Wrist Speed", nameRu = "Скорость кисти",
        descEn = "Speed of the dominant wrist at contact. Indicator of arm acceleration and power. (10-25 m/s)",
        descRu = "Скорость доминантной кисти в контакте (10–25 м/с).",
        lowEn = "Accelerate through the ball — whip the wrist up at contact.",
        lowRu = "Ускоряйся сквозь мяч — хлёст кистью вверх.",
        highEn = "Great racket speed — keep control.",
        highRu = "Отличная скорость — держи контроль.",
        proNote = "Alcaraz: 1.0x | Djokovic: 1.1x | Sinner: 1.2x",
    )
    val DOM_ARM_EXTENSION = MetricDef(
        id = "DOM_ARM_EXTENSION", kind = ValueKind.DEGREES, proMin = 102f, proMax = 180f,
        nameEn = "Arm Extension", nameRu = "Вытянутость руки",
        descEn = "Arm angle at contact point. Full extension for maximum reach. Elbow slightly bent, not locked. (100-180 / 109-180)",
        descRu = "Угол руки в контакте: полное вытяжение, локоть чуть согнут (102–180°).",
        lowEn = "Extend your arm more. You need more extension at contact. Open your arm more away from your body.",
        lowRu = "Больше вытянись в контакте. Открой руку от корпуса.",
        highEn = "Adjust your elbow angle. Stay relaxed.",
        highRu = "Подстрой угол локтя. Расслабься.",
        proNote = "(M3) Alcaraz: 165 | Sinner: 164 | Djokovic: 168 | Federer: 151",
    )
    val NON_DOM_ELBOW = MetricDef(
        id = "NON_DOM_ELBOW", kind = ValueKind.DEGREES, proMin = 102f, proMax = 180f,
        nameEn = "Non-Dominant Elbow", nameRu = "Недоминантный локоть",
        descEn = "Non-dominant arm position during follow-through. Should tuck in. (Non-dom elbow 102-180)",
        descRu = "Недоминантная рука в проводке — прижимается к корпусу (102–180°).",
        lowEn = "Lower your free arm. It should drop after the follow-through.",
        lowRu = "Опусти свободную руку после проводки.",
        highEn = "Tuck the non-dominant arm in during the follow-through.",
        highRu = "Прижми недоминантную руку в проводке.",
    )
    val BACK_LEG_LANDING = MetricDef(
        id = "BACK_LEG_LANDING", kind = ValueKind.DEGREES, proMin = 130f, proMax = 175f,
        nameEn = "Back Leg Landing", nameRu = "Задняя нога при приземлении",
        descEn = "Back leg angle during landing. Stable landing on front foot. (130-162 / 140-175)",
        descRu = "Угол задней ноги при приземлении (130–175°).",
        lowEn = "Land stable on the front foot; the back leg follows.",
        lowRu = "Приземляйся устойчиво на переднюю ногу.",
        highEn = "Back leg kick is fine — stay balanced.",
        highRu = "Мах задней ногой в порядке — держи баланс.",
    )

    // ---- Groundstrokes ---------------------------------------------------------------
    val UNIT_TURN = MetricDef(
        id = "UNIT_TURN", kind = ValueKind.DEGREES, proMin = 40f, proMax = 90f,
        nameEn = "Unit Turn", nameRu = "Замах (скрутка)",
        descEn = "Core rotation during the backswing. Full rotation creates the coil for power.",
        descRu = "Ротация корпуса в замахе — создаёт пружину для мощности.",
        lowEn = "Start the unit turn earlier — shoulders away from the net.",
        lowRu = "Начинай замах раньше — плечи прочь от сетки.",
        highEn = "Slight over-rotation — face the net a bit sooner on the drive.",
        highRu = "Чуть перекручен — раскрывайся раньше.",
    )
    val WRIST_OVER_HIP = MetricDef(
        id = "WRIST_OVER_HIP", kind = ValueKind.RATIO, proMin = 0.0f, proMax = 0.05f, fmt = "%.2f",
        nameEn = "Wrist Over Hip", nameRu = "Кисть над бедром",
        descEn = "Wrist position relative to hip during preparation. Wrist should be above hip.",
        descRu = "Кисть относительно бедра в подготовке — кисть выше бедра.",
        lowEn = "Keep your wrist above hip level during preparation.",
        lowRu = "Держи кисть выше уровня бедра в подготовке.",
        highEn = "Wrist position is fine.",
        highRu = "Позиция кисти в порядке.",
    )
    val ARMS_POSITION_G = MetricDef(
        id = "ARMS_POSITION", kind = ValueKind.DEGREES, proMin = 20f, proMax = 170f,
        nameEn = "Arms Position", nameRu = "Позиция рук",
        descEn = "Both arms position during the stroke. Guides racket and balances body.",
        descRu = "Позиция обеих рук — ведёт ракетку и балансирует корпус.",
        lowEn = "Your hands are too low. Keep both hands up at chest height.",
        lowRu = "Руки слишком низко. Держи у груди.",
        highEn = "Adjust your arm position.",
        highRu = "Подстрой позицию рук.",
    )
    val CONTACT_IN_FRONT = MetricDef(
        id = "CONTACT_POINT", kind = ValueKind.RATIO, proMin = 0.15f, proMax = 0.9f, fmt = "%.2f",
        nameEn = "Contact In Front", nameRu = "Контакт впереди",
        descEn = "Forward distance of contact point from the body (shoulder widths).",
        descRu = "Расстояние контакта впереди корпуса (в ширинах плеч).",
        lowEn = "Contact is late. Meet the ball further out in front.",
        lowRu = "Поздний контакт. Встречай мяч дальше перед собой.",
        highEn = "Contact too far — stay compact and hit closer.",
        highRu = "Слишком далеко — играй компактнее.",
        proNote = "Alcaraz: 28cm (ratio 0.8x) | Sinner: 66cm (ratio 1.9x)",
    )
    val TORSO_ROTATION = MetricDef(
        id = "TORSO_ROTATION", kind = ValueKind.DEGREES, proMin = 3f, proMax = 20f,
        nameEn = "Torso Rotation", nameRu = "Ротация торса",
        descEn = "Trunk rotation toward the court at contact. (Trunk rotation 3-20cm)",
        descRu = "Ротация торса к корту в контакте (3–20см).",
        lowEn = "Rotate into the shot — hips and shoulders to the ball.",
        lowRu = "Провернись в удар — бёдра и плечи к мячу.",
        highEn = "Rotation is fine — keep the head still.",
        highRu = "Ротация в порядке — держи голову неподвижно.",
    )
    val RACQUET_PATH = MetricDef(
        id = "RACQUET_PATH", kind = ValueKind.DEGREES, proMin = 0f, proMax = 45f,
        nameEn = "Racquet Path", nameRu = "Траектория ракетки",
        descEn = "Smooth trajectory during the swing. (var<45deg, dist>=20cm) — requires racket detection.",
        descRu = "Плавность траектории (разброс <45°, путь ≥20см) — требуется детекция ракетки.",
        lowEn = "Smooth the swing path — one clean line to the ball.",
        lowRu = "Сгладить траекторию — одна чистая линия к мячу.",
        highEn = "Path is smooth.",
        highRu = "Траектория плавная.",
    )

    /** Stance classification — a separate entry, not a numeric metric. */
    val STANCE_TYPE = MetricDef(
        id = "STANCE_TYPE", kind = ValueKind.RATIO, proMin = 0f, proMax = 360f,
        nameEn = "Stance Classification", nameRu = "Классификация стойки",
        descEn = "Open or neutral stance classification at contact. Closed or semi-open at backhand contact.",
        descRu = "Классификация стойки на контакте: открытая/нейтральная (закрытая/полузакрытая на бэкхенде).",
        lowEn = "", lowRu = "", highEn = "", highRu = "",
        proNote = "Djokovic: Semi-open | Alcaraz: Open | Sinner: Semi-open; BH: Djokovic: Closed | Sinner: Semi-open | Wawrinka: Closed",
    )

    val ALL: Map<String, MetricDef> = listOf(
        KNEE_FLEXION, DOM_KNEE, NDOM_KNEE, ANKLE_HIP_SHOULDER, HEEL_HIP_SHOULDER, STANCE_WIDTH,
        TRUNK_TILT, SHOULDERS_LEVEL, BODY_CENTER,
        M1_ARMS_POSITION, M1_FOOT_ANGLE, M2_KNEES_POSITION, M2_UNIT_TURN, M2_WEIGHT_TRANSFER,
        M3_TRIPLE_EXTENSION, M3_TROPHY_POSITION, M4_TRUNK_ROTATION, M5_CONTACT_POINT_HEIGHT_DISTANCE,
        M5_RACKET_HEAD_POSITION, TOSS_ARM_EXTENSION, TOSS_ARM_SPEED, WRIST_SPEED, DOM_ARM_EXTENSION,
        NON_DOM_ELBOW, BACK_LEG_LANDING,
        UNIT_TURN, WRIST_OVER_HIP, ARMS_POSITION_G, CONTACT_IN_FRONT, TORSO_ROTATION, RACQUET_PATH,
        STANCE_TYPE,
    ).associateBy { it.id }

    /**
     * Metric layout per phase — as in the original: module codes for each phase.
     * M5/M4 are the most heavily weighted.
     */
    fun layout(stroke: StrokeType): Map<Moment, List<Pair<MetricDef, Float>>> = when (stroke) {
        StrokeType.SERVE -> mapOf(
            Moment.M0 to listOf(KNEE_FLEXION to 1f, M1_ARMS_POSITION to 1f, STANCE_WIDTH to 1f),
            Moment.M1 to listOf(ANKLE_HIP_SHOULDER to 1.2f, DOM_KNEE to 1f, NDOM_KNEE to 1f, M1_FOOT_ANGLE to 1.1f),
            Moment.M2 to listOf(TOSS_ARM_EXTENSION to 1.3f, M2_KNEES_POSITION to 1.2f, M2_UNIT_TURN to 1.1f, TOSS_ARM_SPEED to 1.0f),
            Moment.M3 to listOf(M3_TROPHY_POSITION to 1.4f, M3_TRIPLE_EXTENSION to 1.2f),
            Moment.M4 to listOf(M4_TRUNK_ROTATION to 1.2f, TRUNK_TILT to 1f, M2_WEIGHT_TRANSFER to 1.1f),
            Moment.M5 to listOf(DOM_ARM_EXTENSION to 1.5f, WRIST_SPEED to 1.5f, M5_CONTACT_POINT_HEIGHT_DISTANCE to 1.3f),
            Moment.M6 to listOf(HEEL_HIP_SHOULDER to 1.2f, BACK_LEG_LANDING to 1.1f, NON_DOM_ELBOW to 1f),
        )
        else -> mapOf(
            Moment.M0 to listOf(KNEE_FLEXION to 1f, ARMS_POSITION_G to 1f, STANCE_WIDTH to 1f),
            Moment.M1 to listOf(STANCE_WIDTH to 1.2f, DOM_KNEE to 1f, NDOM_KNEE to 1f),
            Moment.M2 to listOf(UNIT_TURN to 1.4f, WRIST_OVER_HIP to 1.1f, M2_UNIT_TURN to 1.0f),
            Moment.M3 to listOf(M2_WEIGHT_TRANSFER to 1.2f, DOM_KNEE to 1f, TORSO_ROTATION to 1.1f),
            Moment.M4 to listOf(CONTACT_IN_FRONT to 1.5f, DOM_ARM_EXTENSION to 1.2f, WRIST_SPEED to 1.3f, STANCE_TYPE to 1.1f),
            Moment.M5 to listOf(SHOULDERS_LEVEL to 1.2f, NON_DOM_ELBOW to 1f, DOM_ARM_EXTENSION to 1.0f),
            Moment.M6 to listOf(BODY_CENTER to 1f, STANCE_WIDTH to 1f, KNEE_FLEXION to 1f),
        )
    }
}
