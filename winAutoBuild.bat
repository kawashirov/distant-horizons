call ./gradlew clean -PmcVer="1.16.5" --no-daemon
call ./gradlew build -PmcVer="1.16.5" --no-daemon
call ./gradlew merge -PmcVer="1.16.5" --no-daemon

call ./gradlew clean -PmcVer="1.17.1" --no-daemon
call ./gradlew build -PmcVer="1.17.1" --no-daemon
call ./gradlew merge -PmcVer="1.17.1" --no-daemon

call ./gradlew clean -PmcVer="1.18.1" --no-daemon
call ./gradlew build -PmcVer="1.18.1" --no-daemon
call ./gradlew merge -PmcVer="1.18.1" --no-daemon

call ./gradlew clean -PmcVer="1.18.2" --no-daemon
call ./gradlew build -PmcVer="1.18.2" --no-daemon
call ./gradlew merge -PmcVer="1.18.2" --no-daemon

call ./gradlew clean -PmcVer="1.19" --no-daemon
call ./gradlew build -PmcVer="1.19" --no-daemon
call ./gradlew merge -PmcVer="1.19" --no-daemon