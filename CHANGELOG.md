# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [1.4.0] - 2026-11-14

### Added
- A backup file: Settings → More → «Backup» saves every workout, routine, pet, measurement with its photo and the settings into one file, and reads it back on this or a new phone.
- Supersets: «Superset with the next exercise» in an exercise's menu during a workout, and in a routine. The exercises go back to back and the rest timer starts only after the last one. Routines, «Do it again» and «Save as a routine» keep them.
- Achievements: 23 long-term ones with 108 tiers – workouts, weeks to plan, tonnage, records, distance, pull-ups, pets and more. Each tier pays plates. On the gym screen, in the stats and after a workout. Tiers already earned are paid when the app opens.
- 12 more weekly quests: three workouts, records in two exercises, 10 km, an hour of cardio, a superset, a group class and others.
- A workout reminder: pick the days and the time in Settings → Training. One notification, only if you haven't trained that day; quiet on sick leave.
- Health Connect: turn it on in Settings → Profile and finished workouts are written there as exercise sessions for your other health apps. Nothing is read.
- The rest timer can be turned off altogether, and rings when the rest is over (a switch beside it). Where Android holds exact alarms back, Settings offers to allow them, so the signal comes on time with the screen off.
- The weather in the gym window can be turned off: the window then shows only the time of day and the app doesn't go online.

### Fixed
- «Тяга к груди узким хватом» now finds the close-grip pulldown (#11).
- The end of a rest buzzed twice when the app was open.
- Finishing a workout is saved in one piece: a crash in the middle can no longer feed the pets twice.
- Pets grow and the week's quests change even if the app stayed open overnight.
- Deleting a workout marks the records after it again.

<!-- tg
Новое: суперсеты – в меню упражнения «Суперсет со следующим», таймер отдыха запустится только после последнего упражнения.
Достижения: 23 штуки со ступенями, за каждую дают блины. Плюс 12 новых заданий недели.
Резервная копия в файл (Настройки → Ещё), напоминание о тренировке по дням, запись тренировок в Health Connect.
Таймер отдыха теперь можно выключить совсем, а в конце отдыха он звонит. Погоду за окном зала тоже можно выключить.
-->

## [1.3.0] - 2026-11-13

### Added
- Group classes (#10): «Group class» in the start sheet – pilates, stretching, yoga, circuit, HIIT, tabata, dance, aqua aerobics, kickboxing, martial arts or anything else, with a name if you like. Only a timer runs; there's nothing to log as it goes. At the end you mark how hard it was (1–10), how it went and which muscles worked; exercises can still be logged with «Log exercises too». The pets eat by the minutes and the effort, the class counts for the week, the muscle map and recovery, and «Do it again» starts the same class.

<!-- tg
Новое: групповые занятия. Выбери пилатес, растяжку, йогу, круговую или что-то своё – пойдёт только таймер, записывать по ходу ничего не нужно. В конце отметь, насколько было тяжело и какие мышцы работали.
-->

## [1.2.1] - 2026-11-13

### Fixed
- «Lift 5 tonnes this week» counts the same kilos as «lifted» in the journal: own-weight exercises (push-ups, pull-ups, dips…) count too, not only barbells and dumbbells (#9). A quest this week's workouts already met is paid when the app opens.

<!-- tg
Исправлено: задание «Поднять 5 тонн за неделю» считает тот же вес, что и «поднято» в журнале, – вместе с отжиманиями, подтягиваниями и другими упражнениями со своим весом. Если на этой неделе уже набрано, задание засчитается при открытии приложения.
-->

## [1.2.0] - 2026-11-12

- How to do each exercise: every one of the 275 has a short description of the technique, on its page and on its card.
- Edit a past workout: change sets and exercises, the name, the note, the day, the start and how long it took. Records are worked out again; what the pets ate stays.
- Left and right apart: one-arm and one-leg exercises can be logged as L/R pairs; the next time and routines keep the pairs.
- Recovery on the gym screen shows resting and ready muscles together on one figure, with both lists under it.
- Fixed: «How to log it» gave pull-ups as the example for every bodyweight exercise; now the example names the exercise itself.

<!-- tg
Pumpling 1.2.0: у каждого упражнения появилось описание, как его выполнять. Оно есть на странице упражнения и в его карточке.
Прошедшую тренировку теперь можно изменить: подходы, упражнения, название, дату и длительность. Упражнения на одну руку или ногу можно записывать отдельно для левой и правой стороны.
В восстановлении уставшие и отдохнувшие мышцы показаны на одной схеме, а списки под ней.
Исправили подсказку «Как записывать»: раньше там у всех упражнений были подтягивания.
-->

## [1.1.0] - 2026-11-11

- 275 exercises instead of 140, each with a picture of a pet doing it; search understands other names and gym slang («разведение ног», «гравитрон»).
- Target muscles everywhere: tap any exercise picture – in a workout, a routine, a ready routine, the picker – for a card with the muscle map, how to log it and your best.
- Routines show what they work on one body, last time's sets for each exercise, how often you've done them, and a start button.
- A past workout reads like a story: the leading pet, plates and food earned, records in gold, the muscles worked, sets as chips.
- The journal: weeks as single cards with a day strip and a goal mark, older weeks folded, this month in three numbers.
- The gym: your leading pet comments on how training goes, and the next routine waits under «На сегодня»; recovery is always at hand.
- A new pet page: name and badges under the picture, poses, numbers, traits, what it ate, its place on the team.
- Fixed: eggs found while finishing a workout are dated by that workout.

<!-- tg
Pumpling 1.1.0: упражнений стало 275, и у каждого есть картинка. Поиск понимает другие названия, например «разведение ног» или «гравитрон».
Нажмите на картинку упражнения в любом месте, и откроется карточка с целевыми мышцами. В программах видно, что они тренируют, и что вы делали в прошлый раз.
Обновили журнал, страницы тренировки и питомца. В зале ведущий питомец подсказывает, как идут дела, а следующая программа ждёт в «На сегодня».
-->

## [1.0.0] - 2026-11-11

The first release of Pumpling.

- A workout log for weights, bodyweight, assisted, holds, cardio and timed activities: last time's sets with a nudge to beat them, records marked ahead, warm-up sets and RPE, a rest timer in the notification, a plate calculator, workouts logged afterwards.
- 140 exercises with search and filters, your own exercises and your own equipment.
- 33 ready routines for the gym, home and outdoors at three levels, with a look inside before taking one; your own routines; save any workout as a routine. A start always asks first.
- Twenty pets with abilities and natures: a team of three eats from every set and grows on rest days; eggs are found by how you train; grown pets take a build; rare colourings.
- A basement gym furnished with plates, its window showing the real time of day and weather in your town; weekly quests; sick leave.
- Stats by week, month, year and all time: highlights, muscles and balance, each exercise's progress, cardio and best efforts, records, strength standards and DOTS, weight and measurements with photos, a year in review.
- Fourteen kinds of share cards in feed and story sizes.
- An onboarding survey (where you train, your equipment, goals and level) that suggests the first routines; a reminder to weigh in and measure; first-visit hints on every screen.

<!-- tg
Pumpling – дневник тренировок с питомцами. Записывайте подходы, веса и кардио, а команда в маленьком зале тренируется вместе с вами и растёт в дни отдыха.
20 питомцев со своими способностями, яйца находятся в зависимости от того, как вы тренируетесь. 140 упражнений, свои упражнения и инвентарь, 33 готовые программы для зала, дома и улицы.
Много статистики: мышцы, прогресс каждого упражнения, рекорды, вес и замеры. 14 видов открыток для ленты и сторис.
-->
