(ns jira.sandbox.ui
  (:require
    [jira.text :as tt]
    [rum.core :include-macros true :as rum]))




(defonce location (atom "4,Регенераторна,Kiev,UA"))
(defonce g-key (str "key=" "AIzaSyAFfMaK609tANRWpajdLjq0bBmyDCCk7ws"))

(defn png-url []
  (let [base "https://maps.googleapis.com/maps/api/staticmap?"
        size "size=700x500"
        scale "scale=2"
        maptype "maptype=terrain"
        zoom "zoom=11"
        pin-color "color:yellow"
        pin-address @location
        pin-icon "icon:https://goo.gl/nURJvC"
        markers (str "markers=" pin-color "|" pin-address "|" pin-icon)]
    (or (str base "&"
             size "&"
             scale "&"
             maptype "&"
             markers "&"
             zoom "&"
             g-key)
      (str "http://placehold.it/700x300"))))


(declare map-search)
(rum/defcs map-search <
  (rum/local "")
  [state]
  (println (pr-str state))
  (let [v (:rum/local state)
        on-change (fn [e]
                    (reset! v (-> e .-target .-value)))
        on-submit #(reset! location @v)]
    [:form.form-horizontal
     {:role "form"}
     [:div.form-group.required
         [:label.control-label.col-sm-4
          {:for "map-search"}
          (tt/t :address)]
         [:div.col-sm-6
          [:input.form-control#map-search
           {:type        "text"
            :placeholder (tt/t :hint-search-location)
            :value       @v
            :required    "required"
            :on-change   on-change}]]
         [:button.btn.btn-default
          {:type     "submit"
           :on-click on-submit}
          (tt/t :search)]]]))


(declare google-static-map-form)
(rum/defc google-static-map-form
  []
  [:div.col-md-5
    [:h3 "Google Static Map"]
    [:p "You'll get the static png image from Google"]
    [:h4 "Search the venue by address"]
    [:p "You'll get the static png image from Google"]
    (map-search)
    [:a.btn.btn-primary {:href "https://developers.google.com/maps/documentation/static-maps/intro"}
     "View API documentation" [:span.glyphicon.glyphicon-chevron-right]]])


(declare google-static-map)
(rum/defc google-static-map < rum/reactive
  []
  (rum/react location)
  [:div
    [:div.row
     [:div.col-md-7
      [:a {:href "#"}
       [:img.img-responsive {:src (png-url)
                             :alt "Venue Google Map"}]]]
     (google-static-map-form)]])

;;;; ============== DYNAMIC -======

(def d-search (atom "Kiev"))

(defn map-url
  []
  (let [base "https://www.google.com/maps/embed/v1/search?"
        search @d-search
        query #(if (empty? search) "q=kiev" (str "q=" search))]

    (str base "&"
         (query) "&"
         g-key)))





(declare dynamic-map)
(declare google-dynamic-map-form)




(declare google-dynamic-map-search)
(rum/defcs google-dynamic-map-search <
  (rum/local "")
  [state]
  (println (pr-str state))
  (let [state state
        v (:rum/local state)
        on-change (fn [e]
                    (reset! v (-> e .-target .-value)))
        on-submit #(reset! d-search @v)]
        ; #(reset! d-search @v)]
    [:form.form-horizontal
     {:role "form"}
     [:div.form-group.required
      [:label.control-label.col-sm-4
       {:for "map-search"}
       (tt/t :address)]
      [:div.col-sm-6
       [:input.form-control#map-search
        {:type        "text"
         :placeholder (tt/t :hint-search-location)
         :value       @v
         :required    "required"
         :on-change   on-change}]]
      [:button.btn.btn-default
       {:type     "submit"
        :on-click on-submit}
       (tt/t :search)]]]))


(rum/defc google-dynamic-map-form
  []
  [:div.col-md-5
   [:h3 "Google Dynamic Map"]
   [:p "You'll get google map centered by your pin"]
   [:h4 "Search the venue by address or name"]
   [:p "You'll get google map centered by your pin"]
   (google-dynamic-map-search)
   [:a.btn.btn-primary {:href "https://developers.google.com/maps/documentation/embed/start"}
    "View API documentation" [:span.glyphicon.glyphicon-chevron-right]]])


(rum/defc dynamic-map < rum/reactive
  []
  (rum/react d-search)
  ;(let [search @d-search]
  [:div
       [:div.row
        [:div.col-md-7
         [:iframe {:width "100%"
                   :height "500px"
                   :frameborder "1"
                   :src (map-url)}]]
        (google-dynamic-map-form)]])




(declare sandbox)
(rum/defc sandbox
  []
  [:div.container#admin
   [:div.row
    [:div.col-lg-12
     [:h1.page-header "APIs integrations "
      [:small "Test"]]]
    (google-static-map)
    [:h2 ""]
    (dynamic-map)]])




