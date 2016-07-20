(ns jira.core
  (:require
    [jira.users.db :as udb]
    [jira.users.ui :as u]

    [jira.projects.db :as pdb]
    [jira.projects.ui :as p]

    [jira.tickets.db :as tdb]
    [jira.tickets.ui :as t]

    [jira.admin.ui :as a]
    [jira.sandbox.ui :as sand]
    [jira.sandbox.momsapp :as moms]

    [jira.text :as tt]
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

;;;====
(defonce current-page (atom ""))


;;;-----TOGGLES----
(declare moms-page-toggle)
(rum/defc moms-page-toggle
  []
  [:p.navbar-text
   [:a.navbar-link {:href "#"
                    ; :class (if (= (rum/react current-page) "sand-box") "active" "")
                    :on-click #(reset! current-page "moms")}
    "MomsApp" [:span.glyphicon.glyphicon-baby-formula]]])



(declare sbox-page-toggle)
(rum/defc sbox-page-toggle ;< rum/reactive
  []
  [:p.navbar-text
   [:a.navbar-link {:href "#"
                   ; :class (if (= (rum/react current-page) "sand-box") "active" "")
                    :on-click #(reset! current-page "sand-box")}
    "SandBox" [:span.glyphicon.glyphicon-tower]]])


(declare admin-page-toggle)
(rum/defc admin-page-toggle
  []
  [:p.navbar-text.red
   [:a.navbar-link {:href "#"
                    :on-click #(reset! current-page "admin")}
    "ADMIN" [:span.glyphicon.glyphicon-cog]]])


;;=======================

(declare navigation)
(rum/defc navigation < rum/reactive
  []
  (let [user (rum/react udb/current-u)]
    [:nav.navbar.navbar-inverse.navbar-fixed-top
     [:div.container-fluid
      [:div.navbar-form.navbar-header
       [:button {:type          "button"
                 :class         "navbar-toggle collapsed"
                 :data-toggle   "collapse"
                 :data-target   "#navbar"
                 :aria-expanded "false"
                 :aria-controls "navbar"}
        [:span.sr-only "Toggle navigation"]
        [:span.icon-bar]
        [:span.icon-bar]
        [:span.icon-bar]]
       (tt/change-lang)
       [:a.navbar-brand
        {:href "#"
         :on-click #(reset! current-page "jira")}
        "Jira"]]

      [:div.navbar-collapse.collapse {:id "navbar"}
       [:ul.nav.navbar-nav.navbar-right
        [:li (when (udb/is-admin? user) (moms-page-toggle))]
        [:li (when (udb/is-admin? user) (sbox-page-toggle))]
        [:li (when (udb/is-admin? user) (admin-page-toggle))]
        [:li (u/logout-form user)]]]]]))





;;;==========


(declare jira-sap)
(rum/defc jira-sap < rum/reactive
  []
  (rum/react tt/lang)
  (rum/react udb/current-u)
  (rum/react pdb/projects)
  (rum/react pdb/selected-p)
  (rum/react tdb/tickets)
  (rum/react current-page)
  (if (nil? @udb/current-u)
    [:div.main-view
     (u/login-section)]
    [:div.main-view
     (case @current-page
       "login" (u/login-section)
       "admin" [:div
                 (navigation)
                 (a/admin-page)
                 (footer)]
       "sand-box" [:div
                    (navigation)
                    (sand/sandbox)
                    (footer)]
       "moms" (moms/moms-page)
       [:div
         (navigation)
         (p/title-section)
         (p/card-section)
         (t/tickets-table)
         (footer)])]))

(rum/mount (jira-sap) app-div)