# Test sul box Android TV

Runner diagnostico senza librerie aggiuntive, eseguito su DAZN TV Box / Android 10.
Richiede l'app **debug** firmata con la stessa chiave del runner e un box di prova
con antenna, un solo tuner con canali già sintonizzati e permesso READ_TV_LISTINGS concesso.
Non eseguirlo sulla TV in uso: chiude il processo dell'app a fine prova.

```sh
export ANDROID_HOME="$HOME/Library/Android/sdk"
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
sh tests/device/build.sh
adb -s IP_DEL_BOX:5555 install -r /tmp/tv-reorder-device-tests/tests.apk
adb -s IP_DEL_BOX:5555 shell am instrument -w -e mode callbacks com.tvreorder.devicetests/.SmokeRunner
```

Modalità:

- `inspect`: legge il database accessibile all'app, conta canali visibili e controlla lo stato video.
- `callbacks`: simula nel helper dell'app la perdita della sorgente dei canali e il ritorno tramite
  `onInputUpdated`, `onTvInputInfoUpdated` e `onInputStateChanged`. Verifica zero canali durante
  la perdita, recupero dei canali e del video. Riporta a DEFAULT lo stato del componente launcher
  prima della prova per intercettare le terminazioni dovute a `setComponentEnabledSetting`.
  Include una riapertura durante l'assenza e due notifiche di aggiunta ripetute,
  verificando che il conteggio non cambi. Non scollega fisicamente il tuner e non altera
  il database dei canali.
- `setup`: nasconde localmente un canale e invoca il completamento della configurazione,
  verificando che torni visibile. Ripristina le preferenze di visibilità iniziali.
  Non esegue la scansione delle frequenze.
- `smoke`: sei cambi canale, cinque passaggi Home/app e tre cicli sleep/wakeup,
  con stato video e conteggio dei canali dopo ogni operazione. Lo standby può
  interrompere la rete/ADB: in tal caso riconnettere il box e controllare i log. Il risultato
  viene anche salvato nel file privato dell'app `files/device-test-results.txt`
  (leggibile con `adb -s IP_DEL_BOX:5555 shell run-as com.tvreorder.tv cat files/device-test-results.txt`).

L'esito è `failures=0`, senza campo `error`, con `INSTRUMENTATION_CODE: -1`.
`isVideoAvailable` è il segnale inviato dal servizio TV: non sostituisce una
verifica visiva dell'uscita HDMI. Scrcpy può mostrare nero con video hardware sideband.
