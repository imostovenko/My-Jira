(ns jira.tickets.ui
  (:require

    [jira.users.db :as udb]
    [jira.tickets.db :as tdb]
    [jira.projects.db :as pdb]

    [jira.text :as tt]
    [jira.components :as comp]

    [rum.core :include-macros true :as rum]
    [clojure.string :as str]
    [ajax.core :refer [GET POST]]))


(enable-console-print!)


(declare conf-t-delete)
(rum/defc conf-t-delete
  [t-id on-dismiss-fn]
  (let [t-id t-id on-dismiss-fn on-dismiss-fn
        on-submit #(tdb/delete-t! t-id)]
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


(declare new-t-popup)
(rum/defcs new-t-popup <
  (rum/local {:type :0 :status :0 :prior :0 :subj "" :descr "" :assi ""} ::ticket)
  (rum/local "" ::error)
  [state on-close-fn]
  (println (pr-str state))
  (let [state state on-close-fn on-close-fn
        v (::ticket state)
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
        on-submit #(let [res (tdb/create-t! @pdb/selected-p
                                           (:type @v)
                                           (:prior @v)
                                           (:status @v)
                                           (:subj @v)
                                           (:descr @v)
                                           (udb/u-login->id (:assi @v)))
                         err (get res :error)]
                     (if err
                       (reset! error err)
                       (on-close-fn)))]
    (when (empty? (:assi @v)) (swap! v assoc :assi @udb/current-u))
    [:div.overlay
     [:div.popup
      [:div.popup-header
       [:h3 (tt/t :create-new-ticket)]
       [:a.close
        {:on-click on-close-fn}
        "x"]]
      [:div.popup-content
       [:form.form-horizontal
        {:role "form"}
        (comp/my-btn-group (tt/t :type) :type tdb/t-type on-type-change v)
        (comp/my-btn-group (tt/t :status) :status tdb/t-status on-status-change v)
        (comp/my-btn-group (tt/t :priority) :prior tdb/t-prior on-prior-change v)
        [:div.form-group.required
         [:label.control-label.col-sm-4
          {:for "t-subj"}
          (tt/t :subject)]
         [:div.col-sm-6
          [:input.form-control#t-subj
           {:type        "text"
            :placeholder (tt/t :hint-subject)
            :value       (:subj @v)
            :required    "required"
            :on-change   on-subj-change}]]]
        [:div.form-group.required
         [:label.control-label.col-sm-4
          {:for "users"}
          (tt/t :assignee)]
         [:div.col-sm-6
          [:select.form-control#users
           {:on-change on-assi-change
            :value     (:assi @v)}
           (for [i (keys @udb/users)]
             [:option ((@udb/users i) :login)])]]]
        [:div.form-group
         [:label.control-label.col-sm-4
          {:for "t-descr"}
          (tt/t :ticket-detail)]
         [:div.col-sm-6
          [:textarea.form-control
           {:type        "text"
            :id          "t-descr"
            :rows        "3"
            :placeholder (tt/t :hint-ticket-detail)
            :value       (:descr @v)
            :on-change   on-descr-change}]]]]
       (when show-error? (comp/alert-error @error on-alert-dismiss))]
      [:div.popup-footer
       [:div
        [:button.btn.btn-default
         {:type     "button"
          :on-click on-close-fn}
         (tt/t :cancel)]
        [:button.btn.btn-success
         {:type     "submit"
          :on-click on-submit}
         (tt/t :save)]]]]]))


(declare edit-t-popup)
(rum/defcs edit-t-popup <
  (rum/local {:type nil :status nil :prior nil :subj nil :descr nil :assi nil})
  [state t-id on-dismiss-fn]
  (println "modal editTick" (pr-str state))
  (let [state state t-id t-id on-dismiss-fn on-dismiss-fn
        v (:rum/local state)
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
        on-submit #(do (tdb/update-t-descr! t-id (:descr @v))
                       (tdb/update-t-subj! t-id (:subj @v))
                       (tdb/reassign-t! t-id (:assi @v))
                       (tdb/update-t-status! t-id (:status @v))
                       (tdb/update-t-prior! t-id (:prior @v))
                       (tdb/update-t-type! t-id (:type @v)))
        delete #(tdb/delete-t! t-id)
        when-nil-fn (fn [a-key t-key]
                      (when (nil? (a-key @v)) (swap! v assoc a-key (t-key (tdb/get-t t-id)))))]
    (when-nil-fn :type :type)
    (when-nil-fn :status :status)
    (when-nil-fn :prior :prior)
    (when-nil-fn :subj :subj)
    (when-nil-fn :descr :description)
    (when (nil? (:assi @v)) (swap! v assoc :assi (udb/u-id->login (:assignee (tdb/get-t t-id)))))
    [:div.overlay
     [:div.popup
      [:div.popup-header
       [:h3 (tt/t :edit-ticket) t-id]
       [:a.close
        {:on-click on-dismiss-fn}
        "x"]]
      [:div.popup-content
       [:form.form-horizontal {:role "form"}
        (comp/my-btn-group (tt/t :type) :type tdb/t-type on-type-change v)
        (comp/my-btn-group (tt/t :status) :status tdb/t-status on-status-change v)
        (comp/my-btn-group (tt/t :priority) :prior tdb/t-prior on-prior-change v)
        [:div.form-group.required
         [:label.control-label.col-sm-4
          {:for "t-subj"}
          (str (tt/t :subject) " :")]
         [:div.col-sm-6
          [:input.form-control#t-subj
           {:type        "text"
            :placeholder (tt/t :hint-subject)
            :value       (:subj @v)
            :required    "required"
            :on-change   on-subj-change}]]]
        [:div.form-group.required
         [:label.control-label.col-sm-4
          {:for "users"}
          (str (tt/t :assignee) " :")]
         [:div.col-sm-6
          [:select.form-control#users
           {:on-change on-assi-change
            :value     (:assi @v)}
           (for [i (keys @udb/users)]
             [:option ((@udb/users i) :login)])]]]
        [:div.form-group
         [:label.control-label.col-sm-4
          {:for "t-descr"}
          (str (tt/t :ticket-detail) " :")]
         [:div.col-sm-6
          [:textarea.form-control
           {:type        "text"
            :id          "t-descr"
            :rows        "3"
            :placeholder (tt/t :hint-ticket-detail)
            :value       (:descr @v)
            :on-change   on-descr-change}]]]]]
      [:div.popup-footer
       [:div
        [:button.btn.btn-danger.pull-left
         {:type     "button"
          :on-click #((delete) (on-dismiss-fn))}
         (tt/t :delete)]
        [:button.btn.btn-default
         {:type     "button"
          :on-click on-dismiss-fn}
         (tt/t :cancel)]
        [:button.btn.btn-success
         {:type     "submit"
          :on-click #((on-submit) (on-dismiss-fn))}
         (tt/t :save)]]]]]))


(declare filter-section)
(rum/defc filter-section
  [f-settings]
  (let [f-settings f-settings]
    [:div.container-fluid.filter-section
     (comp/filter-group f-settings (tt/t :by-type) tdb/t-type :types)
     (comp/filter-group f-settings (tt/t :by-priority) tdb/t-prior :priors)
     (comp/filter-group f-settings (tt/t :by-status) tdb/t-status :statuses)
     (comp/filter-group f-settings (tt/t :by-assignee) @udb/users :users)]))


(declare t-line)
(rum/defcs t-line < rum/reactive
  (rum/local false ::show-edit?)
  (rum/local false ::show-conf?)
  [state ticket]
  (rum/react tdb/tickets)
  (let [state state ticket ticket
        show-edit? (::show-edit? state)
        toggle-edit #(swap! show-edit? not)
        show-conf? (::show-conf? state)
        toggle-conf #(swap! show-conf? not)
        t-id (:id ticket)
        status (:status ticket)
        type (:type ticket)
        prior (:prior ticket)
        login-name #(:login (@udb/users (% ticket)))]
    [:tr
     [:th t-id]
     [:th
      (cond
        (= type :0) [:p [:span.glyphicon.glyphicon-ice-lolly.green] (:0 tdb/t-type)]
        (= type :1) [:p [:span.glyphicon.glyphicon-ice-lolly-tasted.red] (:1 tdb/t-type)])]
     [:th
      (cond
        (= prior :0) [:span.red (prior tdb/t-prior)]
        (= prior :1) [:span.orange (prior tdb/t-prior)]
        (= prior :2) [:span (prior tdb/t-prior)])]
     [:th.text-capitalize (:subj ticket)]
     [:th.text-capitalize (login-name :assignee)]
     [:th.text-capitalize (login-name :creator)]
     [:th
      (cond
        (= status :0) [:span.label.label-default.text-capitalize (:0 tdb/t-status)]
        (= status :1) [:span.label.label-warning.text-capitalize (:1 tdb/t-status)]
        (= status :2) [:span.label.label-success.text-capitalize (:2 tdb/t-status)])]
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


(declare tickets-lines)
(rum/defcs tickets-lines <
  (rum/local :id ::key)
  (rum/local > ::comparator)
  [state tickets-map]
  (let [state state tickets-map tickets-map
        K (::key state)
        C (::comparator state)
        sorted-tickets (sort-by @K @C tickets-map)
        s (fn [k c]
            (reset! K k)
            (reset! C c))]
    [:table.table.table-condensed
     [:thead
      [:tr
       (comp/col-sortable (tt/t :id) :id s @K @C)
       (comp/col-sortable (tt/t :type) :type s @K @C)
       (comp/col-sortable (tt/t :priority) :prior s @K @C)
       (comp/col-sortable (tt/t :subject) :subj s @K @C)
       (comp/col-sortable (tt/t :assignee) :assignee s @K @C)
       (comp/col-sortable (tt/t :creator) :creator s @K @C)
       (comp/col-sortable (tt/t :status) :status s @K @C)
       [:th (tt/t :actions)]]]
     [:tbody
      (for [ticket sorted-tickets]
        (t-line ticket))]]))


(declare tickets-table)
(rum/defcs tickets-table < rum/reactive
  (rum/local false ::show-create?)
  (rum/local false ::show-filter?)
  (rum/local "" ::search-query)
  (rum/local {:types #{} :priors #{} :statuses #{} :users #{}} ::filter-settings)
  [state]
  (rum/react pdb/selected-p)
  (rum/react tdb/tickets)
  (rum/react pdb/projects)
  (let [state state
        f-settings (::filter-settings state)
        s-query (::search-query state)
        show-create? (::show-create? state)
        toggle-create #(swap! show-create? not)
        show-filter? (::show-filter? state)
        toggle-filter #(do
                        (reset! s-query "")
                        (swap! show-filter? not)
                        (reset! f-settings {:types #{} :priors #{} :statuses #{} :users #{}}))
        empty-tickets-table? (or (empty? (pdb/get-u-projects @udb/current-u))
                               (not (pdb/p-exists? @pdb/selected-p)))
        tickets (tdb/filter-by-project)
        on-search-change (fn [e]
                          (reset! f-settings {:types #{} :priors #{} :statuses #{} :users #{}})
                          (reset! s-query (-> e .-target .-value))
                          (tdb/search @s-query tickets))
        search-results (vals (tdb/search @s-query tickets))
        filter-results (vals (tdb/super-filter @f-settings tickets))

        prj-has-no-tickets? (= (pdb/count-p-tickets @pdb/selected-p) 0)
        nothing-filtered? (= (pdb/count-p-tickets @pdb/selected-p) (count filter-results))
        nothing-searched? (empty? @s-query)]
    (println state)
    [:div.tickets#tickets-table
     (when-not empty-tickets-table?
       [:div.container
        [:div.container-fluid.tt-header
         [:div.col-md-4
          [:h4 [:strong (pdb/get-p-title @pdb/selected-p)] (tt/t :tickets)]]
         [:div.col-md-3
          [:input.form-control.pull-right
           {:type        "search"
            :placeholder (tt/t :hint-search)
            :value       @s-query
            :on-change   on-search-change}]]
         [:div.col-md-3
          [:span "or  "]
          [:button.btn.btn-default
           {:type     "checkbox"
            :value    ""
            :on-click toggle-filter}
           [:span.glyphicon.glyphicon-filter] (tt/t :filter)
           (when-not nothing-filtered? [:span.badge (count filter-results) " X"])]]
         [:div.col-md-2
          [:button.btn.btn-success
           {:type     "button"
            :on-click toggle-create}
           [:span.glyphicon.glyphicon-plus]
           (tt/t :new-ticket)]]]

        (when @show-filter?
          (filter-section f-settings))

        [:div.container-fluid
         (cond
           (true? prj-has-no-tickets?)
           (comp/warning (tt/t :warn-no-tickets-yet))

           (empty? filter-results)
           (comp/warning (tt/t :warn-no-tickets-filter))

           (empty? search-results)
           (do
             (reset! show-filter? false)
             (comp/warning (tt/t :warn-no-tickets-search)))

           (false? nothing-searched?)
           (do
             (reset! show-filter? false)
             (tickets-lines search-results))

           (false? nothing-filtered?)
           (tickets-lines filter-results)

           :else
           (tickets-lines (vals tickets)))

         (when @show-create?
           (new-t-popup toggle-create))]])]))