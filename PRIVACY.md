# Pumpling — Privacy Policy

*Last updated: 13 November 2026*

Pumpling is a workout log for Android with pets that train alongside you.
It is published as open source at <https://github.com/bigdevwhale/pumpling>.

## What we collect

**Nothing.** Pumpling has no accounts, no servers of its own, no analytics, no advertising and no crash reporting.
Nothing you log in the app reaches us.

## What leaves the phone

Only what the gym window needs to show the real weather, over HTTPS:

- **The town.** On first start the app guesses your town from the phone's time zone (for example «Europe/Berlin» → Berlin)
  and looks it up in Open-Meteo's geocoding service (<https://open-meteo.com>). If you search for a town in Settings,
  the name you type is sent to the same service. The app does not ask for your location and never uses GPS.
- **The weather.** The chosen town's coordinates are sent to Open-Meteo, or to MET Norway (<https://api.met.no>)
  when Open-Meteo can't be reached, at most about once every half hour while the app is in use.

Nothing else goes with these requests, and none of them carries an identifier of you or your phone.

The weather can be turned off in Settings → Look → «Outside the gym window». Then the window shows only the time of
day, worked out on the phone, and the app makes no network requests at all.

## What stays on your device

In the app's private storage: your workouts, sets and records, routines and your own exercises, pets and the gym,
body weight and measurements, progress photos, the chosen town and your settings.

Progress photos are taken with your phone's own camera app and kept inside Pumpling. The app doesn't ask for the
camera permission.

## Sharing

- **Cards** (a workout, a record, the week and others) leave the phone only when you share them yourself through
  Android's share sheet, or save them to your pictures.
- **Links** in Settings (bug reports, wishes, the community on Telegram, the StillCraft support page, the source code)
  open Telegram or your browser only when you tap them; what you do there is governed by those services' own policies.
- **Backups.** Android may include the app's data in your own device backup to your Google account, if you have
  that turned on in system settings.
- **A backup file.** Settings → More → «Backup» writes everything the app keeps into one file, in the place you pick.
  The file is yours: the app sends it nowhere, and reads one back only when you choose it.
- **Health Connect.** Off unless you turn it on in Settings → Profile. Then each finished workout is written to
  Health Connect on your phone as an exercise session: when it started and ended, what kind it was, its name and
  note. Other apps see it only if you allow them to in Health Connect. Pumpling reads nothing from Health Connect.
  Editing or deleting a workout changes or removes its session; sessions already written stay there after you turn
  the switch off or uninstall the app, and can be deleted in Health Connect.

## Notifications

The rest timer shows a count-down notification, and rings and buzzes when the rest is over. If you pick days for a
workout reminder, one notification comes on those days. Both are made on the phone; nothing is sent anywhere.

## Deleting your data

Uninstalling the app deletes everything it stored. Nothing is kept anywhere else.

## Health information

Pumpling is not a medical app. The weight, measurements and workouts you log are kept only on the phone and are
never sent anywhere. The one exception is the one you make yourself: with Health Connect turned on, finished workouts
are written to Health Connect on the same phone, as described above.

## Children

Pumpling is not directed at children under 13.

## Changes

Any change to this policy will be published on this page with a new date.

## Contact

Questions: open an issue at <https://github.com/bigdevwhale/pumpling/issues> or write to the StillCraft bot on
Telegram, <https://t.me/ArcaneWhisperBot>.

---

# Pumpling — Политика конфиденциальности

*Обновлено: 13 ноября 2026*

Pumpling — дневник тренировок для Android с питомцами, которые тренируются вместе с вами.
Код открыт: <https://github.com/bigdevwhale/pumpling>.

## Что мы собираем

**Ничего.** В приложении нет аккаунтов, собственных серверов, аналитики, рекламы и отчётов о сбоях.
Ничего из того, что вы записываете, к нам не попадает.

## Что уходит с телефона

Только то, что нужно окну зала, чтобы показать настоящую погоду, по HTTPS:

- **Город.** При первом запуске приложение угадывает город по часовому поясу телефона (например, «Europe/Moscow» → Москва)
  и ищет его в геокодере Open-Meteo (<https://open-meteo.com>). Если вы ищете город в настройках, туда же уходит
  введённое название. Доступа к местоположению приложение не просит и GPS не использует.
- **Погода.** Координаты выбранного города уходят в Open-Meteo, а если он недоступен — в MET Norway
  (<https://api.met.no>), не чаще примерно раза в полчаса, пока приложение открыто.

Больше в этих запросах ничего нет, и ни в одном нет идентификатора вас или телефона.

Погоду можно выключить: Настройки → Вид → «За окном зала». Тогда окно показывает только время суток, оно считается
на телефоне, и приложение вообще не обращается к сети.

## Что хранится на устройстве

В собственном хранилище приложения: тренировки, подходы и рекорды, программы и свои упражнения, питомцы и зал,
вес и замеры, фото прогресса, выбранный город и настройки.

Фото прогресса снимает обычная камера телефона, а хранятся они внутри Pumpling. Разрешения на камеру приложение не просит.

## Ссылки и обмен

- **Открытки** (тренировка, рекорд, неделя и другие) покидают телефон, только когда вы сами делитесь ими через
  системное меню «Поделиться» или сохраняете в галерею.
- **Ссылки** в настройках (ошибки, пожелания, сообщество в Telegram, страница поддержки StillCraft, исходный код)
  открывают Telegram или браузер, только когда вы на них нажимаете; там действуют правила этих сервисов.
- **Резервная копия.** Android может включить данные приложения в резервную копию устройства в вашем
  аккаунте Google, если она включена в настройках.
- **Файл с копией.** Настройки → Ещё → «Резервная копия» записывает всё, что хранит приложение, в один файл — туда,
  куда вы укажете. Файл ваш: приложение никуда его не отправляет и читает обратно, только когда вы сами его выберете.
- **Health Connect.** Выключено, пока вы не включите в Настройки → Профиль. Тогда каждая законченная тренировка
  записывается в Health Connect на вашем телефоне как занятие: когда началась и закончилась, какого вида, название
  и заметка. Другие приложения видят её, только если вы разрешите им это в Health Connect. Из Health Connect
  Pumpling ничего не читает. Правка или удаление тренировки меняет или убирает её запись; уже записанное остаётся
  там, когда вы выключаете переключатель или удаляете приложение, и удаляется в самом Health Connect.

## Уведомления

Таймер отдыха показывает уведомление с обратным отсчётом, а когда отдых закончился — звонит и вибрирует. Если выбрать
дни для напоминания о тренировке, в эти дни придёт одно уведомление. Оба делаются на телефоне; никуда ничего не отправляется.

## Удаление данных

При удалении приложения стирается всё, что оно хранило. Больше нигде ничего не остаётся.

## Данные о здоровье

Pumpling — не медицинское приложение. Вес, замеры и тренировки, которые вы записываете, хранятся только на телефоне
и никуда не отправляются. Единственное исключение делаете вы сами: если включить Health Connect, законченные тренировки
записываются в Health Connect на том же телефоне, как описано выше.

## Дети

Приложение не предназначено для детей младше 13 лет.

## Изменения

Изменения этой политики публикуются на этой странице с новой датой.

## Контакты

Вопросы — через issue: <https://github.com/bigdevwhale/pumpling/issues> или боту StillCraft в Telegram:
<https://t.me/ArcaneWhisperBot>.
