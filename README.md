# À propos de l'application Formulaire

* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt) - Copyright Ville de Paris, Région Nouvelle Aquitaine, Région Hauts de France
* Développeur(s) : CGI
* Financeur(s) : Ville de Paris, Région Nouvelle Aquitaine, Région Hauts de France
* Description : Application de création et de gestion de formulaires dans l'OPEN ENT.

## Configuration du sous-module Formulaire

<pre>
{
  "config": {
    ...
    "zimbra-max-recipients": ${zimbraMaxRecipients},
    "rgpd-cron": "${rgpdCron}",
    "notify-cron": "${notifyCron}",
    "max-responses-export-PDF": ${maxResponsesExportPDF},
    "max-users-sharing": ${maxUsersSharing}
  }
}
</pre>

Dans votre springboard, vous devez inclure des variables d'environnement :

<pre>
zimbraMaxRecipients = Integer
rgpdCron = String (exemple: 0 0 0 */1 * ? *)
notifyCron = String (exemple: 0 0 0 */1 * ? *)
maxResponsesExportPDF = Integer
maxUsersSharing = Integer
</pre>

Dans ce meme fichier inclure "formulaire" dans la liste "ressourcesApplications"

Il y aura aussi à ajouter des variables d'environnement dans votre ent-core.json afin d'utiliser l'export pdf :

<pre>
{
  "config": {
     ...
     "node-pdf-generator" : {
        "pdf-connector-id": "exportpdf",
        "auth": String,
        "url" : String
     }
  }
}
</pre>


## Entcore - archive APi import/export

Attention, afin de faire fonctionner les imports d'archives .zip (import gérés par l'entcore),
il faut ajouter les propriétés suivantes dans l'ent-core.json :
Dans le module entcore-archive :
<pre>
{
  "name": "org.entcore~archive~4...",
  "config": {
    ...
    "publicConf": {
      "apps": {
        "formulaire": "fr.openent.formulaire.controllers.FormController|initCreationRight"
      }
    } 
  }
}
</pre>


## Configuration du sous-module Formulaire-public

Le "db-schema" doit être identique à celui du sous-module Formulaire (utilisation de la même base de donnée).

Aucune autre configuration particulière.