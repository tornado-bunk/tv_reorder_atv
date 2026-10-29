# Verifiche sul DAZN TV Box — 23 ottobre 2026

Dispositivo reale Sagemcom DAZN, Android 10, antenna collegata. App `com.tvreorder.tv`,
APK debug 1.2 (versionCode 3). Sorgente `com.sagemcom.stb.tvinput/.StbTvInputService/HW42`.

## Difetti riprodotti e corretti

- Dopo una perdita temporanea della sorgente, i canali tornavano ma la riproduzione restava ferma.
  La schermata ora riprende la TV quando tornano canali disponibili.
- Una riapertura mentre la sorgente era assente avviava lo stato del player senza un canale.
  Ora il percorso senza canali torna alle impostazioni senza avviare il player.
- Due notifiche di aggiunta ripetute portavano la lista da 305 a 915 elementi.
  Ora vengono reinseriti solo i canali precedentemente esclusi per rimozione della sorgente.
- Aggiornare lo stato del componente launcher poteva far terminare il processo della TV;
  l'aggiunta delle sorgenti esterne non aggiornava il launcher senza BuiltInTunerManager.
  I callback ora aggiornano il launcher anche per tuner esterni, con DONT_KILL_APP.
- Il completamento della scansione saltava la selezione dei canali perché un flag costante era falso.
  Ora una scansione completata rende visibili i canali della sorgente configurata.
- Aggiornamenti di sorgenti assenti dalla cache potevano lasciare incompleto il conteggio delle fonti.
  Ora inizializzano la sorgente e notificano anche i consumatori dei canali. La schermata Fonti
  si aggiorna anche al cambiamento dello stato di connessione.

## Prove eseguite

| Prova | Esito |
| --- | --- |
| Installazione, primo avvio e concessione READ_TV_LISTINGS | Superata |
| Lettura iniziale e riproduzione | 208 canali, video 1920×1080 disponibile |
| Sei cambi canale (tre avanti, tre indietro) | Superata |
| Cinque passaggi Home/app | Superata |
| Tre cicli sleep/wakeup | Superata |
| Riordino con OK / giù / OK | Rai 2 spostato al numero 1, Rai 1 al 2 |
| Riavvio completo del box | Video disponibile e ordine personalizzato conservato |
| Ripristino dell'ordine iniziale | Eseguito prima della nuova scansione |
| Scansione reale tramite interfaccia Sagemcom, salvataggio e rientro in Fonti | 305 canali trovati e visibili |
| Perdita sorgente + onInputUpdated | 0 → 305 canali e ripresa del video |
| Perdita sorgente + onTvInputInfoUpdated | 0 → 305 canali e ripresa del video |
| Perdita sorgente, riapertura a sorgente assente + onInputStateChanged | 0 → 305 canali e ripresa del video |
| Due notifiche onInputAdded ripetute | Restano 305 canali |
| Launcher riportato a DEFAULT prima dei callback | Processo non terminato durante il recupero |
| Canale nascosto localmente + completamento configurazione | 304 → 305 canali visibili; preferenze ripristinate |
| Revoca READ_TV_LISTINGS e riapertura | Permesso richiesto nuovamente e riconcesso |
| Ripetizione finale dei sei cambi canale, cinque ritorni Home e tre standby | Tutte le verifiche superate, 305 canali e video disponibile |
| Guida programmi dopo nuova scansione | Popolata con canali e programmi |
| Selezione numerica 5 + OK | Canale5 HD selezionato |

I test sui callback simulano gli eventi all'interno dell'helper dell'app: non disabilitano
il servizio Sagemcom e non scollegano fisicamente il tuner. La scansione e il riavvio
sono stati invece eseguiti realmente sul box.

Il video viene presentato tramite superficie hardware sideband: scrcpy cattura l'interfaccia
ma mostra nero sul video. La disponibilità del video è stata verificata tramite callback
TV, risoluzione delle tracce e conferma dell'utente sull'uscita reale.

Durante le prove il runner ha avuto un timeout di UiAutomation con i servizi di accessibilità.
La modalità di connessione è stata adeguata; il giro finale è stato ripetuto con `failures=0`.

Runner ripetibile: [tests/device](../tests/device/README.md). Le prove sul dispositivo
non richiedono la suite Robolectric AOSP; i test Robolectric aggiunti restano non eseguiti qui.

APK verificato: `build/outputs/apk/debug/tv_reorder_atv-universal-debug.apk`.
SHA-256: `6f48031a87d869a77624d8ef02e8d9817b006db0f018b4e3380ebd0de6cea322`.

La build sul box è firmata con la chiave debug. L'APK release precedente usa una chiave diversa:
per aggiornare un'installazione release conservando i dati occorre firmare con la chiave release originale.
Non è stata disinstallata l'app dell'utente su altri dispositivi.

Non coperti: funzionamento per giorni, perdita fisica del segnale antenna, tutti i 305 canali uno per uno,
DVR, contenuti a pagamento e altre versioni Android. Le prove non garantiscono l'assenza di ulteriori
problemi specifici del firmware.
