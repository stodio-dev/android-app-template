# יומן לקחים: תקלות בנייה ושיטות ייעול

תקלות שנתקלנו בהן בבניית אפליקציות מהתבנית, ושיטות שחוסכות זמן וריצות build. לכל סוכן AI ולכל מפתח.
**לפני שכותבים — קוראים את [`RULES.md`](RULES.md)** (מבנה רשומה, מזהים, מניעת כפילויות).

חיפוש מהיר:
```bash
grep -rn -i "<מילה מהודעת השגיאה>" docs/lessons/
grep -rn "^### BUILD-" docs/lessons/
```

## אינדקס

| מזהה | סוג | כותרת | חל על | מצב |
|---|---|---|---|---|
| BUILD-001 | שיפור | בדיקת קומפילציה בסשן ענן בלי Android SDK | כל האפליקציות, סשן ענן | פעיל |
| CI-001 | תקלה | הבנייה נכשלת בשלב `Debug keystore`: חסר הסוד | כל האפליקציות | פעיל |
| PROC-001 | תקלה | `git commit -a` אחרי `new-app.sh` שומר commit בלי קבצי ה־Java | כל האפליקציות | תוקן בתבנית |
| PROC-002 | תקלה | סשן שנפתח על ריפו התבנית עצמו | כל האפליקציות, סשן ענן | פעיל |

---

## BUILD

### BUILD-001: בדיקת קומפילציה בסשן ענן בלי Android SDK
- **סוג:** שיפור
- **חל על:** כל האפליקציות, סשן ענן
- **מצב:** פעיל
- **סיבה:** בסשן ענן אין Android SDK, ולכן `./gradlew assembleDebug` נכשל. שגיאות קומפילציה מתגלות רק אחרי כמה
  דקות ב־Actions — ובריפו בלי הסוד `DEBUG_KEYSTORE_B64` (ראו CI-001) לא מתגלות בכלל.
- **פתרון:** מקמפלים מול `android-all` של Robolectric (כל מחלקות המערכת של אנדרואיד 15, מ־Maven Central) עם מחלקת
  `R` מדומה שנבנית מהשימושים בקוד. מריצים משורש הריפו; הקבצים נשמרים ב־`build/` (לא נכנס ל־git). הורדה ראשונה
  כ־186MB, ואחריה הבדיקה לוקחת כשנייה:
  ```bash
  V=15-robolectric-13954326                      # Android 15 (API 35) framework classes
  J=build/android-all.jar; O=build/compile-check
  [ -s $J ] || curl -sSfo $J https://repo1.maven.org/maven2/org/robolectric/android-all/$V/android-all-$V.jar
  NS=$(sed -n "s/.*namespace '\(.*\)'.*/\1/p" app/build.gradle); D=$O/stub/${NS//.//}
  rm -rf $O && mkdir -p $D $O/classes
  grep -rhoE '\bR\.[a-z]+\.[A-Za-z0-9_]+' app/src/main/java | sort -u | awk -F. -v ns="$NS" '
    { f[$2] = f[$2] "    public static final int " $3 " = 0;\n" }
    END { print "package " ns ";\npublic final class R {"; for (k in f) printf "  public static final class %s {\n%s  }\n", k, f[k]; print "}" }' > $D/R.java
  javac -nowarn -source 8 -target 8 -encoding UTF-8 -cp $J -d $O/classes $D/R.java $(find app/src/main/java -name '*.java') && echo "compile OK"
  ```
- **מניעה:** מריצים לפני כל push, אחרי `bash core-tests/run.sh`. מה שהבדיקה **לא** תופסת: קריאה ל־API חדש מ־`minSdk`
  (ה־jar הוא API 35), ממשקים מוסתרים (`@hide`, נמצאים ב־jar), שגיאות ב־`res/` וב־manifest, ושגיאות Gradle/AGP —
  אלה מתגלים רק ב־Actions. `-source 8` מקבל lambdas, שהתבנית אוסרת: בודקים בנפרד `grep -rn -- "->" app/src/main/java`.
- **אומת:** קומפילציה של קוד התבנית הצליחה, ושגיאה מכוונת (`LAYOUT_DIRECTION_RTLX`) נתפסה: `error: cannot find symbol`.
  2026-11-22 · `android-all 15-robolectric-13954326` · JDK 21 מקומי (AGP 8.11.1 ו־Gradle 9.1.0 בקבצים, לא הורצו)
- **נצפה:** 2026-11-22 (×1)

## CI

### CI-001: הבנייה נכשלת בשלב `Debug keystore` — חסר הסוד
- **סוג:** תקלה
- **חל על:** כל האפליקציות
- **מצב:** פעיל
- **תסמין:** ריצת `build` ב־Actions נכשלת תוך שניות בשלב `Debug keystore`, וכל השלבים אחריו `skipped` (אין APK ואין
  release): `::error::The DEBUG_KEYSTORE_B64 secret is missing (Settings > Secrets and variables > Actions)`
- **סיבה:** "Use this template" לא מעתיק סודות. כל ריפו חדש צריך את הסוד בעצמו, וגם לריפו התבנית אין אותו.
- **פתרון:** המשתמש מוסיף בריפו: Settings ← Secrets and variables ← Actions ← New repository secret, בשם
  `DEBUG_KEYSTORE_B64`, עם אותו ערך כמו בשאר האפליקציות שלו. אחר כך מריצים שוב (Actions ← הריצה ← Re-run jobs) או דוחפים.
- **מניעה:** סשן לא יכול להגדיר סודות. בתחילת עבודה על ריפו חדש בודקים אם ריצה קודמת נכשלה בשלב הזה
  (`actions/runs` של הריפו) ומבקשים מהמשתמש להוסיף את הסוד **לפני** ה־push הראשון, לא אחריו.
- **אומת:** ריצה 43038756378 בריפו התבנית נכשלה בשלב הזה (2026-11-11), ונבדקה ב־2026-11-22.
  `AGP 8.11.1 · Gradle 9.1.0 · java 21 · ubuntu-26.04/20261101.186.1`
- **ראו גם:** PROC-002
- **נצפה:** 2026-11-22 (×1)

## PROC

### PROC-001: `git commit -a` אחרי `new-app.sh` שומר commit בלי קבצי ה־Java
- **סוג:** תקלה
- **חל על:** כל האפליקציות
- **מצב:** תוקן בתבנית (רלוונטי לאפליקציות שנוצרו לפני התיקון)
- **תסמין:** אחרי `bash tools/new-app.sh ...` ו־`git commit -am "..."`, הפקודה `git ls-files app/src/main/java` לא
  מחזירה כלום: ה־commit לא כולל אף קובץ Java, והקבצים נשארים בדיסק כ־untracked.
- **סיבה:** הסקריפט מעביר את החבילה ב־`git mv` לתיקייה זמנית (`<package>.tmp`) ואז ב־`mv` רגיל לשם הסופי. האינדקס
  מצביע על הנתיב הזמני שכבר לא קיים, והתיקייה החדשה לא במעקב — ו־`-a` לא מוסיף קבצים חדשים.
- **פתרון:** אחרי הסקריפט: `git add -A`, ורק אז `git commit`. מאז התיקון הסקריפט עושה את זה בעצמו בסופו.
- **מניעה:** לפני commit אחרי שינוי שמות או העברת קבצים — `git status --short`: שורות `??` או נתיבים עם `.tmp`
  אומרות שחסר `git add -A`.
- **אומת:** שוחזר בעותק של התבנית (אחרי `commit -am` הרשימה ריקה); אחרי התיקון אותו תהליך שומר את כל הקבצים. 2026-11-22
- **נצפה:** 2026-11-22 (×1)

### PROC-002: סשן שנפתח על ריפו התבנית עצמו
- **סוג:** תקלה
- **חל על:** כל האפליקציות, סשן ענן
- **מצב:** פעיל
- **תסמין:** המשתמש מבקש אפליקציה חדשה, אבל הסשן פתוח על `android-app-template` (ב־`gradle.properties`:
  `app.id=com.stodio.app`). ה־build שם נכשל (CI-001), ודחיפה ל־`main` הייתה דורסת את התבנית לכל האפליקציות הבאות.
- **סיבה:** "Use this template" לא בוצע, או שהסשן נפתח על הריפו הלא נכון.
- **פתרון:** לא דוחפים את האפליקציה ל־`main` של התבנית. עובדים בענף נפרד, ומבקשים מהמשתמש: Use this template ← ריפו
  פרטי חדש ← הסוד `DEBUG_KEYSTORE_B64` ← לתת את הריפו לסשן. אחר כך מעבירים את העבודה לריפו החדש (העתקת הקבצים או
  `cherry-pick`), ושם דוחפים ל־`main`.
- **מניעה:** בתחילת סשן שמתבקשת בו אפליקציה חדשה: אם שם הריפו הוא `android-app-template` או ש־`app.id` עדיין
  `com.stodio.app` — שואלים את המשתמש על ריפו חדש לפני שכותבים קוד.
- **אומת:** קרה ב־2026-11-22 (משחק פאזל תלת־ממדי שהתבקש בסשן על ריפו התבנית).
- **ראו גם:** CI-001
- **נצפה:** 2026-11-22 (×1)
