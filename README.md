# À propos de l'application Formulaire

* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt) - Copyright Ville de Paris, Région Nouvelle Aquitaine, Région Hauts de France
* Développeur(s) : CGI
* Financeur(s) : Ville de Paris, Région Nouvelle Aquitaine, Région Hauts de France
* Description : Application de création et de gestion de formulaires dans l'OPEN ENT.

## Configuration

<pre>
{
  "config": {
    ...
    "zimbra-max-recipients": ${zimbraMaxRecipients},
    "rgpd-cron": "${rgpdCron}",
    "max-responses-export-PDF": ${maxResponsesExportPDF},
    "max-users-sharing": ${maxUsersSharing}
  }
}
</pre>

Dans votre springboard, vous devez inclure des variables d'environnement :

<pre>
zimbraMaxRecipients = Integer
rgpdCron = String
maxResponsesExportPDF = Integer
maxUsersSharing = Integer
</pre>