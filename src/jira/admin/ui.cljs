(ns jira.admin.ui
  (:require
    [jira.users.db :as udb]
    [jira.projects.db :as pdb]
    [jira.tickets.db :as tdb]
    [jira.admin.db :as adb]

    [jira.components :as comp]
    [rum.core :include-macros true :as rum]))


(defonce show-admin-page? (atom false))




(def selected-type (atom "users"))



(declare dashboard-card)
(rum/defc dashboard-card
  [title descr icon table-type]
  [:div.col-xs-6.col-sm-3.placeholder
    [:a {:href "#"
         :on-click #(do
                      (reset! selected-type table-type)
                      (println @selected-type))}
      [:span.glyphicon.dashboard-icon
       {:class icon
        :style {:color (when (= @selected-type table-type) "orange")}}]
      [:h4 title [:span.text-muted descr]]]])





(declare conf-u-delete)
(rum/defc conf-u-delete
  [u-id on-close-fn]
  (let [u-id u-id on-close-fn on-close-fn
        on-submit #(udb/delete-u! u-id)
        popup-header-title (str "Delete User - " (udb/u-id->login u-id) " , id:" u-id)]
      [:div.overlay
       [:div.popup
        (comp/popup-header popup-header-title on-close-fn)
        [:div.popup-content
         [:p "Are you sure want to delete the user?
         This can not be undone."]]
        [:div.popup-footer
         [:div
          [:button.btn.btn-default
           {:type     "button"
            :on-click on-close-fn}
           "Cancel"]
          [:button.btn.btn-danger
           {:type     "submit"
            :on-click #((on-submit) (on-close-fn))}
           "Delete"]]]]]))



(declare edit-u-popup)
(rum/defcs edit-u-popup <
  (rum/local {:login nil :pass nil})
  [state user-id on-dismiss-fn toggle-conf-fn]
  (let [state state user-id user-id on-dismiss-fn on-dismiss-fn toggle-conf-fn toggle-conf-fn
        v (:rum/local state)
        on-change (fn [key e]
                    (swap! v assoc key (-> e .-target .-value)))
        on-login-change #(on-change :login %)
        on-pass-change #(on-change :pass %)
        on-submit-fn #(udb/update-u! user-id (:login @v) (:pass @v))]
    (when (nil? (:login @v)) (swap! v assoc :login (:login (@udb/users user-id))))
    (when (nil? (:pass @v)) (swap! v assoc :pass (:password (@udb/users user-id))))
    (println @v)
    [:div.overlay
     [:div.popup
       (comp/popup-header "Edit User" on-dismiss-fn)
       [:div.popup-content
        [:form.form-horizontal {:role "form"}
          [:div.form-group.required
           [:label.control-label.col-sm-4
            {:for "u-login"}
            "Login:"]
           [:div.col-sm-6
            [:input.form-control#u-login
             {:type        "text"
              :placeholder "user login"
              :value       (:login @v)
              :required    "required"
              :on-change   on-login-change}]]]
          [:div.form-group.required
           [:label.control-label.col-sm-4
            {:for "pass"}
            "Password:"]
           [:div.col-sm-6
            [:input.form-control#pass
             {:type        "password"
              :placeholder "user password"
              :on-change on-pass-change
              :required    "required"
              :value     (:pass @v)}]]]]]
       (comp/popup-footer on-submit-fn on-dismiss-fn toggle-conf-fn)]]))





(declare u-line)
(rum/defcs u-line <
  (rum/local false ::show-edit?)
  (rum/local false ::show-conf?)
  [state user]
  (let [state state user user
        show-edit? (::show-edit? state)
        toggle-edit #(swap! show-edit? not)
        show-conf? (::show-conf? state)
        toggle-conf #(swap! show-conf? not)
        id (:id user)
        login (:login user)
        role (:role user)
        revoke-a #(adb/revoke-admin! login)
        set-a #(adb/set-admin! login)]
    [:tr
     [:th id]
     [:th login]
     [:th role]
     [:th
      (if (= role "admin")
        [:button.btn.btn-sm.btn-danger
         {:on-click revoke-a} "revoke admin"]
        [:button.btn.btn-sm.btn-success
         {:on-click set-a}"set admin"])]
     [:th
      [:a
       {:on-click toggle-edit}
       [:span.glyphicon.glyphicon-edit.orange]]
      [:a.btn-link.red
       {:on-click toggle-conf}
       [:span.glyphicon.glyphicon-remove-circle.red]]]
     (when @show-edit?
       (edit-u-popup id toggle-edit toggle-conf))
     (when @show-conf?
       (conf-u-delete id toggle-conf))]))



(declare dashboard-u-table)
(rum/defcs dashboard-u-table < rum/reactive
  (rum/local :id ::key)
  (rum/local > ::comparator)
  [state]
  (rum/react udb/users)
  (let [K (::key state)
        C (::comparator state)
        sorted-users (sort-by @K @C (vals @udb/users))
        s (fn [k c]
            (reset! K k)
            (reset! C c))]
    (println "sorted users" sorted-users)
    [:div.container-fluid
      [:h2.sub-header "Users table"]
      [:div.table-responsive
       [:table.table.table-striped
        [:thead
         [:tr
          (comp/col-sortable "#" :id s @K @C)
          (comp/col-sortable "Login" :login s @K @C)
          (comp/col-sortable "Role" :role s @K @C)
          [:th "Admin Rights"]
          [:th "Actions"]]]
        [:tbody
         (for [user sorted-users]
              (u-line user))]]]]))





(declare dashboard-p-table)
(rum/defc dashboard-p-table
  []
  [:div.container-fluid
   [:h2.sub-header "Projects table"]
   [:div.table-responsive
    [:table.table.table-striped
     [:thead
      [:tr]
      [:th "#"]
      [:th "Title"]
      [:th "Description"]
      [:th "N Tickets"]
      [:th "N Users"]
      [:th "Creator"]
      [:th "Actions"]]
     [:tbody
      [:tr]
      [:td "1"]
      [:td "2"]
      [:td "3"]
      [:td "4"]
      [:td "5"]
      [:td "6"]
      [:td "7"]]]]])


(declare dashboard-t-table)
(rum/defc dashboard-t-table
  []
  [:div.container-fluid
   [:h2.sub-header "Tickets table"]
   [:div.table-responsive
    [:table.table.table-striped
     [:thead
      [:tr]
      [:th "#"]
      [:th "Subj"]
      [:th "Assignee"]
      [:th "Creator"]
      [:th "Actions"]]
     [:tbody
      [:tr]
      [:td "1"]
      [:td "2"]
      [:td "3"]
      [:td "4"]
      [:td "5"]]]]])


(declare admin-dashboard)
(declare admin-page)
(rum/defc admin-page < rum/reactive
  []
  (rum/react selected-type)
  [:div.container-fluid#admin
   [:div.row
    [:div.col-sm-10.col-sm-offset-1.col-md-10.col-md-offset-1
      (admin-dashboard)
      (case @selected-type
        "users" (dashboard-u-table)
        "projects" (dashboard-p-table)
        "tickets" (dashboard-t-table))]]])



(rum/defc admin-dashboard
  []
  (let [u-count (udb/count-users)
        p-count (pdb/count-projects)
        t-count (tdb/count-tickets)]
    [:div
     [:h1.page-header "Dashboard"]
     [:div.row.placeholders
      (dashboard-card "All Users" u-count "glyphicon-user" "users")
      (dashboard-card "All Projects" p-count "glyphicon-book" "projects")
      (dashboard-card "All Tickets" t-count "glyphicon-flag" "tickets")]]))


;j<script iframe-height="600" iframe-width="100%" iframe-src="https://story.mapme.com/72dc1553-3b62-4968-84e5-c6b1b0652b79" src="https://hosting.mapme.com/story-embed.js"></script>