(ns jira.projects.ui
  (:require
    [jira.users.db :as udb]
    [jira.projects.db :as pdb]

    [jira.text :as tt]
    [jira.components :as comp]


    [rum.core :include-macros true :as rum]
    [clojure.string :as str]))



(enable-console-print!)


(defonce conf-p-del (atom false))

(declare conf-p-delete)
(rum/defc conf-p-delete
  [p-id on-close-fn]
  (let [on-submit #(pdb/delete-p! p-id)
        popup-header-title (str "Delete Project - " (pdb/get-p-title p-id) " , id:" p-id)]
    (when conf-p-del
      [:div.overlay
       [:div.popup
        (comp/popup-header popup-header-title on-close-fn)
        [:div.popup-content
         [:p "Are you sure want to delete the project?
         All it's tickets will be deleted as well and this can not be undone."]]
        [:div.popup-footer
         [:div
          [:button.btn.btn-default
           {:type     "button"
            :on-click on-close-fn}
           "Cancel"]
          [:button.btn.btn-danger
           {:type     "submit"
            :on-click #((on-submit) (on-close-fn))}
           "Delete"]]]]])))


(declare new-p-popup)
(rum/defcs new-p-popup <
  (rum/local {:title "" :descr ""})
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
        on-submit #(let [res (pdb/create-p! (:title @v) (:descr @v))
                         err (get res :error)]
                    (if err
                      (reset! error err)
                      (on-close-fn)))]
    [:div.overlay
     [:div.popup
      (comp/popup-header (tt/t :create-prj) on-close-fn)
      [:div.popup-content
       [:form.form-horizontal {:role "form"}
        [:div.form-group.required
         [:label.control-label.col-sm-4
          {:for "prj-title"}
          (tt/t :title)]
         [:div.col-sm-6
          [:input.form-control
           {:type        "text"
            :id          "prj-title"
            :placeholder (tt/t :hint-title)
            :value       (:title @v)
            :required    "required"
            :on-change   on-title-change}]]]
        [:div.form-group
         [:label.control-label.col-sm-4
          {:for "prj-descr"}
          (tt/t :description)]
         [:div.col-sm-6
          [:textarea.form-control
           {:type        "text"
            :id          "prj-descr"
            :rows        "3"
            :placeholder (tt/t :hint-description)
            :value       (:descr @v)
            :on-change   on-descr-change}]]]]
       (when show-error?
         (comp/alert-error @error on-alert-dismiss))]
      [:div.popup-footer
       [:div
        [:button.btn.btn-default
         {:type     "button"
          :on-click on-close-fn}
         (tt/t :cancel)]
        [:button.btn.btn-success
         {:type     "submit"
          :on-click on-submit}
         (tt/t :create)]]]]]))


(declare title-section)
(rum/defcs title-section < rum/reactive (rum/local false ::show-modal?)
  [state]
  ;(rum/react comp/lang)
  (let [show-local? (::show-modal? state)
        toggle-modal #(swap! show-local? not)]
    [:div.jumbotron
     [:div.container
      [:div
       [:h1#hello (tt/t :hello) (str/capitalize @udb/current-u) " !"]
       [:p (tt/t :greeting)]
       [:button.btn.btn-primary.btn-lg
        {:on-click toggle-modal}
        (tt/t :create-new-prj)]
       (when @show-local?
         (new-p-popup toggle-modal))]]]))


(declare edit-prj-popup)
(rum/defcs edit-prj-popup < (rum/local {:title nil :descr nil})
  [state p-id on-close-fn conf-p-delete-fn]
  (println "modal editPrj:" (pr-str state))
  (let [v (:rum/local state)
        on-change (fn [key e]
                    (swap! v assoc key (-> e .-target .-value)))
        on-title-change (partial on-change :title)
        on-descr-change #(on-change :descr %)
        on-submit #(do (pdb/update-p-descr! p-id (:descr @v))
                       (pdb/update-p-title! p-id (:title @v)))
        ;delete #(db/delete-p! p-id)
        popup-header-title (str (tt/t :edit-prj) (pdb/get-p-title p-id) " , id:" p-id)]
    (when (nil? (:descr @v)) (swap! v assoc :descr (pdb/get-p-descr p-id)))
    (when (nil? (:title @v)) (swap! v assoc :title (pdb/get-p-title p-id)))

    [:div.overlay
     [:div.popup
      (comp/popup-header popup-header-title on-close-fn)
      [:div.popup-content
       [:form.form-horizontal {:role "form"}
        [:div.form-group.required
         [:label.control-label.col-sm-4
          {:for "prj-title"}
          (tt/t :title)]
         [:div.col-sm-6
          [:input.form-control
           {:type        "text"
            :id          "prj-title"
            :placeholder (tt/t :hint-title)
            :value       (:title @v)
            :required    "required"
            :on-change   on-title-change}]]]
        [:div.form-group
         [:label.control-label.col-sm-4
          {:for "prj-descr"}
          (tt/t :description)]
         [:div.col-sm-6
          [:textarea.form-control
           {:type        "text"
            :id          "prj-descr"
            :rows        "3"
            :placeholder (tt/t :hint-description)
            :value       (:descr @v)
            :on-change   on-descr-change}]]]]]
      [:div.popup-footer
       [:div
        [:button.btn.btn-danger.pull-left
         {:type     "button"
          :on-click #((conf-p-delete-fn) (on-close-fn))}
         (tt/t :delete)]
        [:button.btn.btn-default
         {:type     "button"
          :on-click on-close-fn}
         (tt/t :cancel)]
        [:button.btn.btn-success
         {:type     "submit"
          :on-click #((on-submit) (on-close-fn))}
         (tt/t :save)]]]]]))


(declare prj-card)
(rum/defcs prj-card < rum/reactive
  (rum/local false ::show-modal?)
  (rum/local false ::show-conf-delete?)
  [state p-id]
  (println "modal prjCard" (pr-str state))
  (let [state state p-id p-id
        show-local? (::show-modal? state)
        toggle-modal #(swap! show-local? not)
        show-conf-delete? (::show-conf-delete? state)
        toggle-conf #(swap! show-conf-delete? not)]

    [:div.col-md-4
     [:div.card
      [:h2 (pdb/get-p-title p-id)
       (when (= @udb/current-u (pdb/who-is-author p-id))
         [:span.pull-right
          [:a.navbar-link
           {:on-click toggle-modal}
           [:span.glyphicon.glyphicon-pencil.orange]]])]

      (when @show-local?
        (edit-prj-popup p-id toggle-modal toggle-conf))

      (when @show-conf-delete?
        (conf-p-delete p-id toggle-conf))

      [:h4 [:span.small (tt/t :created-by) (str/capitalize (pdb/who-is-author p-id))]]
      [:p (pdb/get-p-descr p-id)]

      [:button.btn.btn-default
       {:type     "button"
        :on-click #(pdb/select-p p-id)}
       (tt/t :view-tickets)
       [:span.badge (pdb/count-p-tickets p-id)]]]]))


(declare card-section)
(rum/defc card-section < rum/reactive
  []
  (rum/react pdb/projects)
  (let [projects (pdb/get-u-projects @udb/current-u)]
    [:div.container.card-section
     (if (empty? projects)
       (comp/warning (tt/t :warn-no-prj))
       (for [p-id projects]
         (prj-card p-id)))]))

