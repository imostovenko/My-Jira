(ns jira.core
  (:require
    [jira.users.db :as udb]
    [jira.users.ui :as u]

    [jira.projects.db :as pdb]
    [jira.projects.ui :as p]

    [jira.tickets.db :as tdb]
    [jira.tickets.ui :as t]

    [jira.admin.ui :as a]
    [rum.core :include-macros true :as rum]))

(enable-console-print!)

(def app-div (js/document.getElementById "app"))

(declare footer)
(rum/defc footer
  []
  [:div.container
   [:hr]
   [:footer.footer
    [:p "© Mostovenko, 2016"]]])


(declare jira-sap)
(rum/defc jira-sap < rum/reactive
  []
  (rum/react udb/current-u)
  (rum/react pdb/projects)
  (rum/react pdb/selected-p)
  (rum/react tdb/tickets)
  (if (nil? @udb/current-u)
    [:div.main-view
     (u/login-section)]
    (if (rum/react a/show-admin-page?)
      [:div.main-view
       (u/navigation)
       (a/admin-page)]
      [:div.main-view
        (u/navigation)
        (p/title-section)
        (p/card-section)
        (t/tickets-table)
        (footer)])))

(rum/mount (jira-sap) app-div)