# Entropy for Life (Unofficial) · app Android

App **non ufficiale** per i video di biologia di [Entropy for Life](https://www.youtube.com/@EntropyforLife) (Giacomo Moro Mauretto).
Non è affiliata al canale: video, nome e immagini sono di Entropy for Life.

- Ricerca fra tutti i video pubblici del canale, sul telefono e senza rete: parole (nel titolo, nella descrizione, nei tag), tema, rubrica, tipo.
- Novità, post del canale e avvisi dei video, short e dirette nuovi.
- Preferiti e playlist, salvati solo sul telefono. Niente account, niente pubblicità, niente tracciamento.
- «I miei canali»: altri canali YouTube da seguire, ognuno con i suoi avvisi.

## Cosa fa in rete

L'app parla solo con YouTube (feed RSS pubblici del canale, miniature, la pagina dei post) e una volta ogni 6 ore legge
`https://mondo.workingwithweb.eu/entropy-for-life/versione.json` per proporre gli aggiornamenti. Il catalogo dei video è
dentro l'app (`app/src/entropy/assets/catalogo.json`); i video nuovi li aggiunge leggendo il feed di YouTube.

## Scaricare l'app

https://mondo.workingwithweb.eu/entropy-for-life-unofficial-app/ · controllo VirusTotal della 4.42.42:
[0 su 64](https://www.virustotal.com/gui/file/23f2bedb777352bb2fda22070aa876c36fce04eda274b5d9c2c252c97b8f1e57)

## Licenza

Codice pubblico per trasparenza, **tutti i diritti riservati** (non è open source): vedi [LICENSE](LICENSE).
Sviluppata da Working With Web.
