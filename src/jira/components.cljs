(ns jira.components
  (:require
    [jira.db :as db]

    [rum.core :include-macros true :as rum]
    [clojure.string :as str]
    [ajax.core :refer [GET POST]]
    [jira.util :as ut]))

(enable-console-print!)




(declare alert-error)
(rum/defc alert-error
  [alert-message on-alert-dismiss]
  [:div.form-group
   [:div.alert.alert-danger.alert-error
    [:span.glyphicon.glyphicon-exclamation-sign.red]
    alert-message
    [:a.close#X {:href "#"
                 :on-click on-alert-dismiss} "X"]]])


(declare warning)
(rum/defc warning
  [warning-message]
  [:div.alert.alert-warning warning-message])




(declare popup-header)
(rum/defc popup-header
  [title on-close-fn]
  [:div.popup-header
   [:h3 title]
   [:a.close {:on-click on-close-fn} "x"]])


(declare popup-footer)
(rum/defc popup-footer
  [on-submit-fn on-dismiss-fn on-delete-fn]
  [:div.popup-footer
   [:div
    [:button.btn.btn-danger.pull-left
     {:type     "button"
      :on-click #((on-delete-fn) (on-dismiss-fn))}
     "Delete"]
    [:button.btn.btn-default
     {:type     "button"
      :on-click on-dismiss-fn}
     "Cancel"]
    [:button.btn.btn-success
     {:type     "submit"
      :on-click #((on-submit-fn) (on-dismiss-fn))}
     "Save"]]])


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



(declare filter-group)
(rum/defc filter-group
  [filter-settings group-title key-group key]
  (let [f-settings filter-settings
        title group-title
        kk key-group
        k key]
    (if (= k :users)
      [:div.col-md-3
       [:h5 title]
       [:ul.list-group
        (for [i (keys kk)]
          (let [contains (contains? (k @f-settings) i)
                f (if contains disj conj)
                toggle #(swap! f-settings update k f i)]
            [:li.list-group-item
             [:label.checkbox-inline
              [:input {:type     "checkbox"
                       :value    ""
                       :on-click toggle
                       :checked  contains}]
              ((kk i) :login)]]))]]
      [:div.col-md-3
       [:h5 title]
       [:ul.list-group
        (for [i (keys kk)]
          (let [contains (contains? (k @f-settings) i)
                f (if contains disj conj)
                toggle #(swap! f-settings update k f i)]
            [:li.list-group-item
             [:label.checkbox-inline
              [:input {:type     "checkbox"
                       :value    ""
                       :on-click toggle
                       :checked  contains}]
              (i kk)]]))]])))


(declare col-sortable)
(rum/defc col-sortable
  [column-title column-key s-fn K C]
  (let [column-title column-title column-key column-key s-fn s-fn K K C C]
    [:th
     [:span.column-title column-title]
     [:span.glyphicon.glyphicon-chevron-up.arrow
      {:on-click #(s-fn column-key >)
       :class (when (and (= K column-key) (= C >)) "pressed")}]
     [:span.glyphicon.glyphicon-chevron-down.arrow
      {:on-click #(s-fn column-key <)
       :class (when (and (= K column-key) (= C <)) "pressed")}]]))
