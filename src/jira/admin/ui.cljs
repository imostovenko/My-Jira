(ns jira.admin.ui
  (:require
    [jira.users.db :as udb]
    [jira.components :as comp]
    [rum.core :include-macros true :as rum]))


(defonce show-admin-page? (atom false))


(declare admin-page-toggle)
(rum/defc admin-page-toggle
  []
  [:p.navbar-text.red
   [:a.navbar-link {:href "#"
                    :on-click #(swap! show-admin-page? not)}
    "ADMIN" [:span.glyphicon.glyphicon-cog]]])


(declare admin-page)
(rum/defc admin-page
  []
  [:div.jumbotron
    [:div.container
     [:div
      [:h1#hello "Hello, You are on ADMIN page"]]]])