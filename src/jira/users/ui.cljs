(ns jira.users.ui
  (:require
    [jira.users.db :as udb]
    [jira.components :as comp]

    [jira.admin.ui :as a]

    [rum.core :include-macros true :as rum]
    [clojure.string :as str]
    [ajax.core :refer [GET POST]]))



(enable-console-print!)


(declare login-form)
(rum/defcs login-form < (rum/local {:e "" :p ""} ::creds)
                        (rum/local "" ::error)
  [state]
  (println (pr-str state))
  (let [v (::creds state)
        error (::error state)
        show-error? (not (str/blank? @error))
        on-alert-dismiss #(reset! error "")
        on-submit #(let [res (udb/login-u! (:e @v) (:p @v))
                         err (:error res)]
                    (when err
                      (reset! error err)))
        on-change (fn [key e]
                    (swap! v assoc key (-> e .-target .-value)))
        on-email-change (partial on-change :e)
        on-passw-change #(on-change :p %)]
      [:div.login-card
       [:h3 "Login Form"]
       [:div.form-group
        [:input.form-control#email
         {:type        "text"
          :placeholder "Email"
          :value       (:e @v)
          :on-change   on-email-change}]]
       [:div.form-group
        [:input.form-control#password
         {:type        "password"
          :placeholder "Password"
          :value       (:p @v)
          :on-change   on-passw-change}]]
       [:div.form-group#buttons
        [:button.btn.btn-success.pull-right
         {:type     "submit"
          :on-click on-submit}
         "Sign In " [:span.glyphicon.glyphicon-star-empty]]
        [:span.pull-left
         [:a {:on-click #(reset! udb/need-registration? 1)
               :vertical-align "middle"}
          "Sign Up"]]]
       (when show-error?
         (comp/alert-error @error on-alert-dismiss))]))




(declare registration-form)
(rum/defcs registration-form < (rum/local {:e "" :p ""})
                               (rum/local "" ::error)
  [state]
  (println (pr-str state))
  (let [v (:rum/local state)
        error (::error state)
        show-error? (not (str/blank? @error))
        on-alert-dismiss #(reset! error "")
        on-submit #(let [res (udb/register-u! (:e @v) (:p @v))
                         err (get res :error)]
                    (if err
                      (reset! error err)))
        on-change (fn [key e]
                    (swap! v assoc key (-> e .-target .-value)))
        on-email-change #(on-change :e %)
        on-passw-change #(on-change :p %)]
    [:div.login-card
     [:h3 "Registration Form"]
     [:div.form-group.required
      [:input.form-control
       {:type        "text"
        :placeholder "Email"
        :value       (:e @v)
        :required    "required"
        :on-change   on-email-change}]]
     [:div.form-group
      [:input.form-control
       {:type        "password"
        :placeholder "Password"
        :value       (:p @v)
        :on-change   on-passw-change}]]
     [:div.form-group
      [:button.btn.btn-success.pull-right
       {:type     "submit"
        :on-click on-submit}
       "Sign Up " [:span.glyphicon.glyphicon-star-empty]]
      [:span.pull-left
       [:a
        {:on-click       #(reset! udb/need-registration? 0)
         :vertical-align "middle"}
        "Sign In"]]]
     (when show-error?
       (comp/alert-error @error on-alert-dismiss))]))




(declare login-footer)
(rum/defc login-footer
  []
  [:div.bottom-align-text.footer
   [:hr]
   [:p "© Mostovenko, 2016"]])




(declare login-section)
(rum/defc login-section < rum/reactive
  []
  (rum/react udb/need-registration?)
  [:div.login-section
    [:div.col-md-4.col-md-offset-4.col-xs-12.col-sm-10.col-sm-offset-1
     (if (= 0 @udb/need-registration?)
       (login-form)
       (registration-form))]
   (login-footer)])




(declare logout-form)
(rum/defc logout-form
  [user]
  [:p.navbar-text.navbar-right "Signed in as "
   [:a.navbar-link {:href     "#"
                    :on-click #(udb/logout-u!)}
    [:i (str/capitalize user)]
    [:span.glyphicon.glyphicon-log-out]]])



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
       [:a.navbar-brand
        {:href "#"}
        "Jira"]]
      [:div.navbar-collapse.collapse {:id "navbar"}
       [:div.navbar-form.navbar-right
        (when (udb/is-admin? user) (a/admin-page-toggle))
        (logout-form user)]]]]))
