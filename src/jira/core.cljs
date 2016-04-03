(ns jira.core
  (:require
    [jira.db :as db]
    [rum.core :include-macros true :as rum]
    [clojure.string :as str]
    [ajax.core :refer [GET POST]]
    [jira.util :as u]))

(enable-console-print!)

(def app-div (js/document.getElementById "app"))

;----- Declare---------

(declare
  new-t-popup
  edit-prj-popup
  confirmation-t-delete
  registration-form
  footer)

;-----------------------


(declare alert-error alert-message on-alert-dismiss)
(rum/defc alert-error
  [alert-message on-alert-dismiss]
  [:div.form-group
   [:div.alert.alert-danger.alert-error
    [:span.glyphicon.glyphicon-exclamation-sign.red]
    alert-message
    [:a.close#X {:href "#"
                 :on-click on-alert-dismiss} "X"]]])


(declare login-form)
(rum/defcs login-form < (rum/local {:e "" :p ""} ::creds)
                        (rum/local false ::error)
  [state]
  (println (pr-str state))
  (let [v (::creds state)

        error (::error state)
        login-error-text #(:error (db/login-u! (:e @v) (:p @v)))
        login-error? (not (nil? login-error-text))
        on-alert-dismiss #(reset! error false)

        on-change (fn [key e]
                    (swap! v assoc key (-> e .-target .-value)))
        on-email-change (partial on-change :e)
        on-passw-change #(on-change :p %)
        on-submit #(do
                    (db/login-u! (:e @v) (:p @v))
                    (reset! error login-error?))]

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
         [:a {:on-click #(reset! db/need-registration? 1)
               :vertical-align "middle"}
          "Sign Up"]]]
       (when @error (alert-error (login-error-text) on-alert-dismiss))]))



(rum/defcs registration-form < (rum/local {:e "" :p ""})
                              (rum/local "" ::error)
  [state]
  (println (pr-str state))
  (let [v (:rum/local state)
        error (::error state)
        show-error? (not (str/blank? @error))
        on-alert-dismiss #(reset! error "")

        on-submit #(let [res (db/register-u! (:e @v) (:p @v))
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
        {:on-click       #(reset! db/need-registration? 0)
         :vertical-align "middle"}
        "Sign In"]]]
     (when show-error?
       (alert-error @error on-alert-dismiss))]))


(declare logout-form)
(rum/defc logout-form
  [user]
  [:p.navbar-text.navbar-right "Signed in as "
   [:a.navbar-link {:href     "#"
                    :on-click #(db/logout-u!)}
    [:i (str/capitalize user)]
    [:span.glyphicon.glyphicon-log-out]]])

(declare login-section)
(rum/defc login-section < rum/reactive
  []
  (rum/react db/need-registration?)
  [:div.login-section
    [:div.col-md-4.col-md-offset-4.col-xs-12.col-sm-10.col-sm-offset-1
     (if (= 0 @db/need-registration?)
       (login-form)
       (registration-form))]
    [:div.bottom-align-text.footer
     [:hr]
     [:p "© Mostovenko, 2016"]]])


(declare navigation)
(rum/defc navigation < rum/reactive
  []
  (let [user (rum/react db/current-u)]
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
        (if-not (empty? user)
          (logout-form user))]]]]))


(rum/defc popup-header
  [title on-close-fn]
  [:div.popup-header
   [:h3 title]
   [:a.close {:on-click on-close-fn} "x"]])


(declare new-p-popup)
(rum/defcs title-section < rum/reactive (rum/local false ::show-modal?)
  [state]
  (let [show-local? (::show-modal? state)
        toggle-modal #(swap! show-local? not)]
    [:div.jumbotron
     [:div.container
      [:div
       [:h1#hello "Hello, " (str/capitalize @db/current-u) " !"]
       [:p "You can create a new project or go to browse tickets
     of your existing projects"]
       [:button.btn.btn-primary.btn-lg
        {:on-click toggle-modal}
        "Create New Project"]
       (when @show-local?
         (new-p-popup toggle-modal))]]]))


(rum/defcs new-p-popup < (rum/local {:title "" :descr ""})
                         (rum/local "" ::error)
  [state on-close-fn]
  (println "modal newPrj:" (pr-str state))

  (let [v (:rum/local state)
        error (::error state)
        show-error? (not (str/blank? @error))
        on-alert-dismiss #(reset! error "")

        on-change (fn [key e]
                    (swap! v assoc key (-> e .-target .-value)))
        on-title-change #(on-change :title %)
        on-descr-change #(on-change :descr %)
        on-submit #(let [res (db/create-p! (:title @v) (:descr @v))
                         err (get res :error)]
                    (if err
                      (reset! error err)
                      (on-close-fn)))]
    [:div.overlay
     [:div.popup
      (popup-header "Create NEW Project" on-close-fn)
      [:div.popup-content
       [:form.form-horizontal {:role "form"}
        [:div.form-group.required
         [:label.control-label.col-sm-4
          {:for "prj-title"}
          "Title:"]
         [:div.col-sm-6
          [:input.form-control
           {:type        "text"
            :id          "prj-title"
            :placeholder "project title"
            :value       (:title @v)
            :required    "required"
            :on-change   on-title-change}]]]
        [:div.form-group
         [:label.control-label.col-sm-4
          {:for "prj-descr"}
          "Description:"]
         [:div.col-sm-6
          [:textarea.form-control
           {:type        "text"
            :id          "prj-descr"
            :rows        "3"
            :placeholder "short project description"
            :value       (:descr @v)
            :on-change   on-descr-change}]]]]
       (when show-error?
         (alert-error @error on-alert-dismiss))]
      [:div.popup-footer
       [:div
        [:button.btn.btn-default
         {:type     "button"
          :on-click on-close-fn}
         "Cancel"]
        [:button.btn.btn-success
         {:type     "submit"
          :on-click on-submit}
         "Create"]]]]]))






(declare conf-p-delete)
(defonce conf-p-del (atom false))

(rum/defcs edit-prj-popup < (rum/local {:title nil :descr nil} ::prj)
  [state p-id on-close-fn conf-p-delete-fn]
  (println "modal editPrj:" (pr-str state))
  (let [v (::prj state)
        on-change (fn [key e]
                    (swap! v assoc key (-> e .-target .-value)))
        on-title-change (partial on-change :title)
        on-descr-change #(on-change :descr %)
        on-submit #(do (db/update-p-descr! p-id (:descr @v))
                       (db/update-p-title! p-id (:title @v)))
        delete #(db/delete-p! p-id)

        popup-header-title (str "Edit Project - " (db/get-p-title p-id) " , id:" p-id)]
    (when (nil? (:descr @v)) (swap! v assoc :descr (db/get-p-descr p-id)))
    (when (nil? (:title @v)) (swap! v assoc :title (db/get-p-title p-id)))

    [:div.overlay
     [:div.popup
      (popup-header popup-header-title on-close-fn)

      [:div.popup-content
       [:form.form-horizontal {:role "form"}
        [:div.form-group.required
         [:label.control-label.col-sm-4
          {:for "prj-title"}
          "Title:"]
         [:div.col-sm-6
          [:input.form-control
           {:type        "text"
            :id          "prj-title"
            :placeholder "project title"
            :value       (:title @v)
            :required    "required"
            :on-change   on-title-change}]]]

        [:div.form-group
         [:label.control-label.col-sm-4
          {:for "prj-descr"}
          "Description:"]
         [:div.col-sm-6
          [:textarea.form-control
           {:type        "text"
            :id          "prj-descr"
            :rows        "3"
            :placeholder "short project description"
            :value       (:descr @v)
            :on-change   on-descr-change}]]]]]

      [:div.popup-footer
       [:div
        [:button.btn.btn-danger.pull-left
         {:type     "button"
          :on-click #((conf-p-delete-fn)(on-close-fn))}
         "Delete"]
        [:button.btn.btn-default
         {:type     "button"
          :on-click on-close-fn}
         "Cancel"]
        [:button.btn.btn-success
         {:type     "submit"
          :on-click #((on-submit) (on-close-fn))}
         "Save"]]]]]))


(declare prj-card)
(rum/defcs prj-card < rum/reactive (rum/local false ::show-modal?) (rum/local false ::show-conf-delete?)
  "Card with details of a single project"
  [state p-id]
  (println "modal prjCard" (pr-str state))
  (let [id p-id
        show-local? (::show-modal? state)
        toggle-modal #(swap! show-local? not)

        show-conf-delete? (::show-conf-delete? state)
        toggle-conf #(swap! show-conf-delete? not)]

    [:div.col-md-4
     [:div.card
      [:h2 (db/get-p-title id)
       (when (= @db/current-u (db/who-is-author id))
         [:span.pull-right
          [:a.navbar-link
           {:on-click toggle-modal}
           [:span.glyphicon.glyphicon-pencil.orange]]])]

      (when @show-local?
        (edit-prj-popup id toggle-modal toggle-conf))

      (when @show-conf-delete?
        (conf-p-delete id toggle-conf))

      [:h4 [:span.small "Created by "] (str/capitalize (db/who-is-author id))]
      [:p (db/get-p-descr id)]

      [:button.btn.btn-default
       {:type     "button"
        :on-click #(db/select-p id)}
       "View tickets "
       [:span.badge (db/count-p-tickets id)]]]]))


(rum/defc card-section < rum/reactive
  []
  (rum/react db/tickets)
  (rum/react db/projects)
  (let [projects (db/get-u-projects @db/current-u)]
    [:div.container.card-section
     (if (empty? projects)
       [:div.alert.alert-warning {:role "alert"}
        [:strong "You don't have projects yet. "]
        "Please create your first one."]
       (for [p-id projects]
         (prj-card p-id)))]))


(declare conf-t-delete)
(rum/defc conf-t-delete
  [t-id on-dismiss-fn]
  (let [on-submit #(db/delete-t! t-id)]
    [:div.overlay
     [:div.popup
      [:div.popup-header
       [:h3 "Delete Ticket - " t-id]
       [:a.close
        {:on-click on-dismiss-fn}
        "x"]]
      [:div.popup-content
       [:p "Are you sure want to delete the ticket? This can not be undone."]]
      [:div.popup-footer
       [:div
        [:button.btn.btn-default
         {:type     "button"
          :on-click on-dismiss-fn}
         "Cancel"]
        [:button.btn.btn-danger
         {:type     "submit"
          :on-click #((on-submit) (on-dismiss-fn))}
         "Delete"]]]]]))


(declare conf-p-delete)
(rum/defc conf-p-delete
  [p-id on-dismiss-fn]
  (let [on-submit #(db/delete-p! p-id)]
    (when conf-p-del
      [:div.overlay
       [:div.popup
        [:div.popup-header
         [:h3 (str "Delete Project - " (db/get-p-title p-id) " , id:" p-id)]
         ;[:h3 "Delete Project - " p-id " - " (:title (db/get-p p-id))]
         [:a.close
          {:on-click on-dismiss-fn}
          "x"]]
        [:div.popup-content
         [:p "Are you sure want to delete the project?
         All it's tickets will be deleted as well and this can not be undone."]]
        [:div.popup-footer
         [:div
          [:button.btn.btn-default
           {:type     "button"
            :on-click on-dismiss-fn}
           "Cancel"]
          [:button.btn.btn-danger
           {:type     "submit"
            :on-click #((on-submit) (on-dismiss-fn))}
           "Delete"]]]]])))






(declare on-dismiss-fn my-btn-group)
(rum/defcs new-t-popup < (rum/local {:type :0 :status :0 :prior :0 :subj "" :descr "" :assi @db/current-u } ::ticket)
                         (rum/local "" ::error)
  [state on-close-fn]
  (println (pr-str state))
  (let [v (::ticket state)
        error (::error state)
        show-error? (not (str/blank? @error))
        on-alert-dismiss #(reset! error "")
        on-change (fn [key e]
                    (swap! v assoc key (-> e .-target .-value)))
        on-subj-change #(on-change :subj %)
        on-descr-change #(on-change :descr %)
        on-assi-change #(on-change :assi %)
        on-status-change (fn [new-status]
                           (swap! v assoc :status new-status))
        on-prior-change (fn [new-prior]
                          (swap! v assoc :prior new-prior))
        on-type-change (fn [new-type]
                         (swap! v assoc :type new-type))
        on-submit #(let [res (db/create-t! @db/selected-p (:type @v) (:prior @v) (:status @v) (:subj @v) (:descr @v) (db/u-login->id (:assi @v)))
                         err (get res :error)]
                     (if err
                       (reset! error err)
                       (on-close-fn)))]
    [:div.overlay
     [:div.popup
      [:div.popup-header
       [:h3 "Create New Ticket"]
       [:a.close
        {:on-click on-close-fn}
        "x"]]

      [:div.popup-content
       [:form.form-horizontal
        {:role "form"}
        (my-btn-group "Type:" :type db/t-type on-type-change v)
        (my-btn-group "Status:" :status db/t-status on-status-change v)
        (my-btn-group "Priority:" :prior db/t-prior on-prior-change v)

        [:div.form-group.required
         [:label.control-label.col-sm-4
          {:for "t-subj"}
          "Subject:"]
         [:div.col-sm-6
          [:input.form-control#t-subj
           {:type        "text"
            :placeholder "ticket subj"
            :value       (:subj @v)
            :required    "required"
            :on-change   on-subj-change}]]]

        [:div.form-group.required
         [:label.control-label.col-sm-4
          {:for "users"}
          "Assignee:"]
         [:div.col-sm-6
          [:select.form-control#users
           {:on-change on-assi-change
            :value     (:assi @v)}
           (for [i (keys @db/users)]
             [:option ((@db/users i) :login)])]]]
        [:div.form-group
         [:label.control-label.col-sm-4
          {:for "t-descr"}
          "Description:"]
         [:div.col-sm-6
          [:textarea.form-control
           {:type        "text"
            :id          "t-descr"
            :rows        "3"
            :placeholder "ticket description"
            :value       (:descr @v)
            :on-change   on-descr-change}]]]]
       (when show-error? (alert-error @error on-alert-dismiss))]

      [:div.popup-footer
       [:div
        [:button.btn.btn-default
         {:type     "button"
          :on-click on-close-fn}
         "Cancel"]
        [:button.btn.btn-success
         {:type     "submit"
          :on-click on-submit}
         "Create"]]]]]))



(declare my-btn-group)
(rum/defc my-btn-group
  [btn-label btn-key values-set on-btn-click v]
  [:div.form-group.required
   [:label.control-label.col-sm-4
    {:for btn-label}
    btn-label]
   [:div.btn-group.col-sm-6
    {:id btn-label}
    (for [i (keys values-set)]
      [:button.btn.btn-default
       {:class (if (= i (btn-key @v)) "active" "")
        :type     "button"
        :value    (values-set (btn-key @v))
        :on-click #(on-btn-click i)}
       (i values-set)])]])


(declare edit-t-popup)
(rum/defcs edit-t-popup < (rum/local {:type nil :status nil :prior nil :subj nil :descr nil :assi nil})
  [state t-id on-dismiss-fn]
  (println "modal editTick" (pr-str state))
  (let [v (:rum/local state)
        on-change (fn [key e]
                    (swap! v assoc key (-> e .-target .-value)))
        on-subj-change #(on-change :subj %)
        on-descr-change #(on-change :descr %)
        on-assi-change #(on-change :assi %)
        on-status-change (fn [new-status]
                           (swap! v assoc :status new-status))
        on-prior-change (fn [new-prior]
                          (swap! v assoc :prior new-prior))
        on-type-change (fn [new-type]
                         (swap! v assoc :type new-type))
        on-submit #(do (db/update-t-descr! t-id (:descr @v))
                       (db/update-t-subj! t-id (:subj @v))
                       (db/reassign-t! t-id (:assi @v))
                       (db/update-t-status! t-id (:status @v))
                       (db/update-t-prior! t-id (:prior @v))
                       (db/update-t-type! t-id (:type @v)))
        delete #(db/delete-t! t-id)
        when-nil-fn (fn [a-key t-key]
                      (when (nil? (a-key @v)) (swap! v assoc a-key (t-key (db/get-t t-id)))))]
    (when-nil-fn :type :type)
    (when-nil-fn :status :status)
    (when-nil-fn :prior :prior)
    (when-nil-fn :subj :subj)
    (when-nil-fn :descr :description)
    (when (nil? (:assi @v)) (swap! v assoc :assi (db/u-id->login (:assignee (db/get-t t-id)))))

    [:div.overlay
     [:div.popup
      [:div.popup-header
       [:h3 "Edit Ticket - " t-id]
       [:a.close
        {:on-click on-dismiss-fn}
        "x"]]
      [:div.popup-content
       [:form.form-horizontal {:role "form"}
        (my-btn-group "Type:" :type db/t-type on-type-change v)
        (my-btn-group "Status:" :status db/t-status on-status-change v)
        (my-btn-group "Priority:" :prior db/t-prior on-prior-change v)
        [:div.form-group.required
         [:label.control-label.col-sm-4
          {:for "t-subj"}
          "Subject:"]
         [:div.col-sm-6
          [:input.form-control#t-subj
           {:type        "text"
            :placeholder "ticket subj"
            :value       (:subj @v)
            :required    "required"
            :on-change   on-subj-change}]]]
        [:div.form-group.required
         [:label.control-label.col-sm-4
          {:for "users"}
          "Assignee:"]
         [:div.col-sm-6
          [:select.form-control#users
           {:on-change on-assi-change
            :value     (:assi @v)}
           (for [i (keys @db/users)]
               [:option ((@db/users i) :login)])]]]
        [:div.form-group
         [:label.control-label.col-sm-4
          {:for "t-descr"}
          "Description:"]
         [:div.col-sm-6
          [:textarea.form-control
           {:type        "text"
            :id          "t-descr"
            :rows        "3"
            :placeholder "ticket description"
            :value       (:descr @v)
            :on-change   on-descr-change}]]]]]
      [:div.popup-footer
       [:div
        [:button.btn.btn-danger.pull-left
         {:type     "button"
          :on-click #((delete) (on-dismiss-fn))}
         "Delete"]
        [:button.btn.btn-default
         {:type     "button"
          :on-click on-dismiss-fn}
         "Cancel"]
        [:button.btn.btn-success
         {:type     "submit"
          :on-click #((on-submit) (on-dismiss-fn))}
         "Save"]]]]]))





(declare filter-section)
(rum/defc filter-section
  [f-settings]
  [:div.container-fluid.filter-section
   [:div.col-md-3
    [:h5 "by Type:"]
    [:ul.list-group
     (for [i (keys db/t-type)]
       (let [contains (contains? (:types @f-settings) i)
             f (if contains disj conj)
             toggle #(swap! f-settings update :types f i)]
         [:li.list-group-item
          [:label.checkbox-inline
           [:input {:type     "checkbox"
                    :value    ""
                    :on-click toggle
                    :checked  contains}]
           (i db/t-type)]]))]]
   [:div.col-md-3
    [:h5 "by Priority:"]
    [:ul.list-group
     (for [i (keys db/t-prior)]
       (let [contains (contains? (:priors @f-settings) i)
             f (if contains disj conj)
             toggle #(swap! f-settings update :priors f i)]
         [:li.list-group-item
          [:label.checkbox-inline
           [:input {:type     "checkbox"
                    :value    ""
                    :on-click toggle
                    :checked  contains}]
           (i db/t-prior)]]))]]
   [:div.col-md-3
    [:h5 "by Status:"]
    [:ul.list-group
     (for [i (keys db/t-status)]
       (let [contains (contains? (:statuses @f-settings) i)
             f (if contains disj conj)
             toggle #(swap! f-settings update :statuses f i)]
         [:li.list-group-item
          [:label.checkbox-inline
           [:input {:type     "checkbox"
                    :value    ""
                    :on-click toggle
                    :checked  contains}]
           (i db/t-status)]]))]]
   [:div.col-md-3
    [:h5 "by Assignee:"]
    [:ul.list-group
     (for [i (keys @db/users)]
       (let [contains (contains? (:users @f-settings) i)
             f (if contains disj conj)
             toggle #(swap! f-settings update :users f i)]
         [:li.list-group-item
          [:label.checkbox-inline
           [:input {:type     "checkbox"
                    :value    ""
                    :on-click toggle
                    :checked  contains}]
           ((@db/users i) :login)]]))]]])





(declare tickets-lines t-line)
(rum/defcs tickets-lines < rum/reactive
  (rum/local nil ::tickets)
  [state filtered-tickets-map]
  (let [tickets (::tickets state)
        sorted-by (fn [key order]
                    (reset! tickets (sort-by key order @tickets)))]
    (when (nil? @tickets) (reset! tickets filtered-tickets-map))
    [:table.table.table-condensed
     [:thead
      [:tr
       [:th "ID"
        [:a {:on-click #(sorted-by :id >)} " desc"]
        [:a {:on-click #(sorted-by :id <)} " asc"]]
       [:th "Type"
        [:a {:on-click #(sorted-by :type >)} " desc"]
        [:a {:on-click #(sorted-by :type <)} " asc"]]
       [:th "Priority"
        [:a {:on-click #(sorted-by :prior >)} " desc"]
        [:a {:on-click #(sorted-by :prior <)} " asc"]]
       [:th "Subject"
        [:a {:on-click #(sorted-by :subj >)} " desc"]
        [:a {:on-click #(sorted-by :subj <)} " asc"]]
       [:th "Assignee"
        [:a {:on-click #(sorted-by :assignee >)} " desc"]
        [:a {:on-click #(sorted-by :assignee <)} " asc"]]
       [:th "Creator"
        [:a {:on-click #(sorted-by :creator >)} " desc"]
        [:a {:on-click #(sorted-by :creator <)} " asc"]]
       [:th "Status"
        [:a {:on-click #(sorted-by :status >)} " desc"]
        [:a {:on-click #(sorted-by :status <)} " asc"]]
       [:th "Actions"]]]
     [:tbody
      (for [ticket @tickets]
        (t-line ticket))]]))


(declare t-line)
(rum/defcs t-line < rum/reactive
  (rum/local false ::show-edit?)
  (rum/local false ::show-conf?)
  [state ticket]

  (let [show-edit? (::show-edit? state)
        toggle-edit #(swap! show-edit? not)
        show-conf? (::show-conf? state)
        toggle-conf #(swap! show-conf? not)
        t-id (:id ticket)
        status (:status ticket)
        type (:type ticket)
        prior (:prior ticket)
        login-name #(:login (@db/users (% ticket)))]
    [:tr
     [:th t-id]
     [:th
      (cond
        (= type :0) [:p [:span.glyphicon.glyphicon-ice-lolly.green] (:0 db/t-type)]
        (= type :1) [:p [:span.glyphicon.glyphicon-ice-lolly-tasted.red] (:1 db/t-type)])]
     [:th
      (cond
        (= prior :0) [:span.red (prior db/t-prior)]
        (= prior :1) [:span.orange (prior db/t-prior)]
        (= prior :2) [:span (prior db/t-prior)])]
     [:th.text-capitalize (:subj ticket)]
     [:th.text-capitalize (login-name :assignee)]
     [:th.text-capitalize (login-name :creator)]
     [:th
      (cond
        (= status :0) [:span.label.label-default.text-capitalize (:0 db/t-status)]
        (= status :1) [:span.label.label-warning.text-capitalize (:1 db/t-status)]
        (= status :2) [:span.label.label-success.text-capitalize (:2 db/t-status)])]
     [:th
      [:a
       {:on-click toggle-edit}
       [:span.glyphicon.glyphicon-edit.orange]]
      [:a.btn-link.red
       {:on-click toggle-conf}
       [:span.glyphicon.glyphicon-remove-circle.red]]]
     (when @show-edit?
       (edit-t-popup t-id toggle-edit))
     (when @show-conf?
       (conf-t-delete t-id toggle-conf))]))




(declare tickets-table)
(rum/defcs tickets-table < rum/reactive
  (rum/local false ::show-create?)
  (rum/local false ::show-filter?)
  (rum/local nil ::search-query)
  (rum/local {:types #{} :priors #{} :statuses #{} :users #{}} ::filter-settings)
  [state]
  (rum/react db/tickets)
  (rum/react db/projects)
  (rum/react db/selected-p)

  (let [f-settings (::filter-settings state)
        s-query (::search-query state)
        show-create? (::show-create? state)
        toggle-create #(swap! show-create? not)
        show-filter? (::show-filter? state)
        toggle-filter #(do
                        (swap! show-filter? not)
                        (reset! f-settings {:types #{} :priors #{} :statuses #{} :users #{}}))
        empty-tickets-table? (or (empty? (db/get-u-projects @db/current-u))
                               (not (db/p-exists? @db/selected-p)))
        tickets (->> (db/get-tt-of-p @db/selected-p)
                  (select-keys @db/tickets))
        on-search-change (fn [e]
                          (reset! f-settings {:types #{} :priors #{} :statuses #{} :users #{}})
                          (reset! s-query (-> e .-target .-value))
                          (db/search @s-query tickets))
        search (db/search @s-query tickets)
        filtered-tickets-map (vals (db/super-filter @f-settings tickets))
        prj-has-no-tickets? (= (db/count-p-tickets @db/selected-p) 0)
        nothing-filtered? (= (db/count-p-tickets @db/selected-p) (count filtered-tickets-map))
        nothing-selected-filter? (and (empty? (:types @f-settings))
                                   (empty? (:priors @f-settings))
                                   (empty? (:statuses @f-settings))
                                   (empty? (:users @f-settings)))]

    (println @s-query)
    (println nothing-selected-filter?)
    (println state)

    [:div.tickets#tickets-table
     (if empty-tickets-table?
       [:div.container]
       [:div.container
        [:div.container-fluid.tt-header
         [:div.col-md-4
          [:h4 [:strong (db/get-p-title @db/selected-p)] " tickets:"]]
         [:div.col-md-3
          [:input.form-control.pull-right
           {:type        "search"
            :placeholder "Search by ticket subj"
            :value       @s-query
            :on-change   #(on-search-change %)}]]

         [:div.col-md-3
          [:span "or  "]
          [:button.btn.btn-default
           {:type     "checkbox"
            :value    ""
            :on-click toggle-filter}
           "Filter  "
           (when-not nothing-filtered? [:span.red [:span.badge (count filtered-tickets-map)] " X"])]]

         [:div.col-md-2
          [:button.btn.btn-success
           {:type     "button"
            :on-click toggle-create}
           [:span.glyphicon.glyphicon-plus]
           "New Ticket"]]]
        (when @show-filter?
          (filter-section f-settings))
        [:div.container-fluid
         (cond
           (true? prj-has-no-tickets?)
           [:div.alert.alert-warning "No tickets for that project yet."]
           (empty? filtered-tickets-map)
           [:div.alert.alert-warning "Sorry, no tickets for your selection, try change the filter params."]
           (empty? search)
           [:div.alert.alert-warning "Sorry, no tickets for your search, try another search params."]
           (empty? @s-query)
           (tickets-lines filtered-tickets-map)
           :else
           (tickets-lines search))
         (when @show-create?
           (new-t-popup toggle-create))
         (when-not nothing-selected-filter?
           (reset! s-query))]])]))



(rum/defc footer
  []
  [:div.container
   [:hr]
   [:footer.footer [:p "© Mostovenko, 2016"]]])


(rum/defc project-page < rum/reactive
  []
  (rum/react db/current-u)
  (if (empty? @db/current-u)
    [:div.main-view
     (login-section)]
    [:div.main-view
     (navigation)
     (title-section)
     (card-section)
     (tickets-table)
     (footer)]))


(rum/mount (project-page) app-div)