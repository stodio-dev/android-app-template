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
| SIGN-001 | תקלה | אין את הערך של המפתח המשותף: מפתח חדש, ו־`app.signer` מחושב לפני ה־push | כל האפליקציות | פעיל |
| GFX-001 | שיפור | בדיקת shaders וגאומטריה ב־WebGL של Chromium, בלי טלפון | משחקים / גרפיקה, סשן ענן | פעיל |
| PROC-001 | תקלה | `git commit -a` אחרי `new-app.sh` שומר commit בלי קבצי ה־Java | כל האפליקציות | תוקן בתבנית (2d2aa2f, 2026-11-22) |
| PROC-002 | תקלה | סשן שנפתח על ריפו התבנית עצמו | כל האפליקציות, סשן ענן | פעיל |
| PROC-003 | תקלה | `cp -r` של worktree: ה־commits בעותק נוחתים במקור | כל האפליקציות, סשן ענן | פעיל |

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
  2026-11-22 · `android-all 15-robolectric-13954326` · JDK 21 מקומי (AGP 8.11.1 ו־Gradle 9.1.0 בקבצים, לא הורצו).
  2026-11-23: משחק שלם (כ־20 מחלקות, OpenGL ES) עבר את הבדיקה ונבנה ב־Actions בניסיון הראשון, בלי שגיאת קומפילציה
  (`stodio-dev/Arrows_Puzzle`, ריצה 45148177699). `AGP 8.11.1 · Gradle 9.1.0 · java 21 · ubuntu-26.04/20261116.202.1`
- **נצפה:** 2026-11-23 (×2)

## CI

### CI-001: הבנייה נכשלת בשלב `Debug keystore` — חסר הסוד
- **סוג:** תקלה
- **חל על:** כל האפליקציות
- **מצב:** פעיל
- **תסמין:** ריצת `build` ב־Actions נכשלת תוך שניות בשלב `Debug keystore`, וכל השלבים אחריו `skipped` (אין APK ואין
  release): `::error::The DEBUG_KEYSTORE_B64 secret is missing (Settings > Secrets and variables > Actions)`. מאז
  `d120a28`: `::error::The DEBUG_KEYSTORE_B64 secret is missing. Add it at <url> (...), then re-run this job.`
- **סיבה:** "Use this template" לא מעתיק סודות. כל ריפו חדש צריך את הסוד בעצמו, וגם לריפו התבנית אין אותו. החשבון
  `<owner>` הוא חשבון אישי ולא ארגון, ולכן אין סוד Actions משותף לכל הריפואים.
- **פתרון:** המשתמש מוסיף בריפו: Settings ← Secrets and variables ← Actions ← New repository secret, בשם
  `DEBUG_KEYSTORE_B64`, עם אותו ערך כמו בשאר האפליקציות שלו. אחר כך מריצים שוב (Actions ← הריצה ← Re-run jobs) או דוחפים.
  מאז `d120a28` (2026-11-23, אחרי ×3 — סעיף 8) הודעת השגיאה וסיכום הריצה מקשרים ישר לדף הוספת הסוד של אותו ריפו
  (`$GITHUB_SERVER_URL/$GITHUB_REPOSITORY/settings/secrets/actions/new`). אפליקציה שנוצרה לפני כן: מעתיקים את שלב
  `Debug keystore` מה־workflow של התבנית. המצב נשאר `פעיל`, כי גם אפליקציה חדשה צריכה להוסיף את הסוד.
- **מניעה:** סשן לא יכול להגדיר סודות. ריפו שנוצר מהתבנית מריץ build כבר על ה־`Initial commit`, וזו נכשלת בשלב הזה
  אם הסוד חסר. לפני ה־push הראשון בודקים את הריצה הזו (`actions/runs` של הריפו); אם נכשלה ב־`Debug keystore` —
  מבקשים מהמשתמש להוסיף את הסוד ומחכים, לא דוחפים.
- **אומת:** ריצה 43038756378 בריפו התבנית נכשלה בשלב הזה (2026-11-11), ונבדקה ב־2026-11-22.
  `AGP 8.11.1 · Gradle 9.1.0 · java 21 · ubuntu-26.04/20261101.186.1`. שוב ב־2026-11-23 בריפו אפליקציה חדש
  (`stodio-dev/Arrows_Puzzle`): ה־push הראשון נדחף בלי לבדוק את ריצת ה־`Initial commit`, ושתיהן נכשלו כאן. הקישור
  החדש נבדק בריצה 45143722918 שם: השגיאה הופיעה עם כתובת הדף של הריפו.
  `AGP 8.11.1 · Gradle 9.1.0 · java 21 · ubuntu-26.04/20261116.202.1`
- **ראו גם:** PROC-002
- **נצפה:** 2026-11-23 (×3)

## SIGN

### SIGN-001: אין את הערך של המפתח המשותף — מפתח חדש, ו־`app.signer` מחושב לפני ה־push
- **סוג:** תקלה
- **חל על:** כל האפליקציות
- **מצב:** פעיל
- **תסמין:** המשתמש לא מוצא את הערך של `DEBUG_KEYSTORE_B64`: GitHub לא מציג סוד אחרי שנשמר (גם לא בריפו אחר שלו), והערך
  לא נשמר בשום מקום אחר. מפתח שאינו זה של `app.signer` נכשל אחרי הבנייה בשלב `Check signature` (לפי ה־workflow).
- **סיבה:** סודות ב־GitHub הם לכתיבה בלבד, ו־`app.signer` של התבנית שייך למפתח אחד מסוים. כל מפתח חדש = תעודה אחרת.
- **פתרון:** לאפליקציה שעוד לא הותקנה בטלפון — מפתח חדש (במחשב, או בסשן):
  ```bash
  keytool -genkeypair -keystore debug.keystore -storetype PKCS12 -alias androiddebugkey -storepass android \
    -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
  base64 -w0 debug.keystore     # הערך לסוד DEBUG_KEYSTORE_B64
  keytool -list -v -keystore debug.keystore -storepass android | sed -n 's/.*SHA256: //p' | tr -d ':' | tr 'A-F' 'a-f'
  ```
  השורה האחרונה היא הערך ל־`app.signer` ב־`gradle.properties`, ונדחפת **לפני** הבנייה הבאה. אותה פקודה על ערך שהמשתמש
  מצא אומרת אם הוא המפתח של `app.signer`. אפליקציה שכבר מותקנת לא תתעדכן עם מפתח אחר: צריך להסיר אותה מהטלפון קודם
  (והנתונים שלה נמחקים).
- **מניעה:** את ה־base64 שומרים ביום היצירה במקום בטוח (מנהל סיסמאות): כל אפליקציה חדשה צריכה אותו. לא מעלים אותו
  לשיחה עם סוכן (הוא נשאר בהיסטוריה). מפתח שהפך למשותף: מעדכנים גם את `app.signer` בתבנית — המשתמש מבקש.
- **אומת:** 2026-11-23 ב־`stodio-dev/Arrows_Puzzle`: ה־SHA-256 חושב מקומית מהמפתח של המשתמש, `app.signer` עודכן לפני
  הבנייה, וריצה 45148177699 עברה את `Check signature` בניסיון הראשון (`signer fb95…56f4`). הפקודות נבדקו על מפתח זמני
  שנמחק. `AGP 8.11.1 · Gradle 9.1.0 · java 21 · ubuntu-26.04/20261116.202.1`
- **ראו גם:** CI-001
- **נצפה:** 2026-11-23 (×1)

## GFX

### GFX-001: בדיקת shaders וגאומטריה ב־WebGL של Chromium, בלי טלפון
- **סוג:** שיפור
- **חל על:** משחקים / גרפיקה, סשן ענן
- **מצב:** פעיל
- **סיבה:** שגיאת GLSL מתגלה רק בטלפון (מסך ריק, וההודעה רק ביומן), ו־winding הפוך או טקסטורה הפוכה לא נתפסים
  בשום בדיקה אחרת. בסשן ענן אין טלפון ואין אמולטור.
- **פתרון:** ה־shaders כמחרוזות במחלקה בלי `android.*`, והגאומטריה נבנית כמערך floats ב־`core`. תוכנית Java קטנה
  כותבת את שניהם ל־JSON, ודף HTML מצייר אותם ב־WebGL 1 (אותה GLSL ES 1.00 של OpenGL ES 2.0) ב־Chromium של
  Playwright שמותקן בסשן: `chromium.launch({args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader']})`, ואז
  `page.screenshot`. הדף מחזיר את `getShaderInfoLog` / `getProgramInfoLog` ואת `gl.getError()`. בנוסף, ב־`core-tests`:
  המכפלה הווקטורית של כל משולש מול הנורמל שלו (winding).
- **מניעה:** מריצים אחרי כל שינוי ב־shader, בגאומטריה או במטריצות, לפני push. לא נבדק כך: הבדלי דרייברים בטלפון
  (precision, מגבלות uniforms), וציור ה־Canvas של אנדרואיד (הטקסטורה בדף היא קירוב).
- **אומת:** שגיאה מכוונת נתפסה עם השורה: `ERROR: 0:20: '+' : wrong operand types`; shaders תקינים צוירו נכון. מימוש
  מלא: `tools/gl-preview/` (`run.sh`, `page.html`, `shot.js`) באפליקציה קוביות חץ. 2026-11-22
- **נצפה:** 2026-11-22 (×1)

## PROC

### PROC-001: `git commit -a` אחרי `new-app.sh` שומר commit בלי קבצי ה־Java
- **סוג:** תקלה
- **חל על:** כל האפליקציות
- **מצב:** תוקן בתבנית (2d2aa2f, 2026-11-22)
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

### PROC-003: `cp -r` של worktree — ה־commits בעותק נוחתים ב־worktree המקורי
- **סוג:** תקלה
- **חל על:** כל האפליקציות, סשן ענן
- **מצב:** פעיל
- **תסמין:** אחרי `cp -r <worktree> <copy>` ו־commit בעותק (למשל כדי לנסות סקריפט), ב־worktree המקורי
  `git branch --show-current` מראה את ענף הניסיון, `git log` את ה־commits שלו, ו־`git status` שינויים שלא נעשו שם.
- **סיבה:** ב־worktree, `.git` הוא קובץ (`gitdir: <repo>/.git/worktrees/<name>`) ולא תיקייה, אז העותק משתמש באותו
  HEAD ובאותו אינדקס כמו המקור.
- **פתרון:** במקור: `git diff` (לוודא שהקבצים בדיסק הם שלכם), `git reset -q <commit שהיה>` (מחזיר HEAD ואינדקס,
  משאיר קבצים), `git checkout -q <הענף שהיה>` או `--detach`, ואז `git branch -D <ענף הניסיון>`.
- **מניעה:** עותק לניסוי רק ב־`git clone <path> <dir>` (עם `.git` משלו) או ב־`git worktree add`, לא ב־`cp -r`.
  בדיקה מהירה: `test -f .git && echo "worktree: do not cp -r"`.
- **אומת:** קרה ב־2026-11-22 בבדיקת `tools/new-app.sh` על עותק; שוחזר כמתואר, ושום דבר לא נדחף.
- **נצפה:** 2026-11-22 (×1)
