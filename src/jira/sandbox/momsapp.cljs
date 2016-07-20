(ns jira.sandbox.momsapp
  (:require
    [jira.text :as tt]
    [rum.core :include-macros true :as rum]))

(def showmap (atom false))
(def viewall (atom false))

(declare moms-header)
(rum/defc moms-header
  []
  [:div
   [:nav.navbar.navbar-default.navbar-fixed-top.moms-header
    [:div.container-fluid
     [:div.row
      [:div.col-md-1.col-sm-1.col-xs-2.pull-left
       [:span.glyphicon.glyphicon-menu-hamburger.moms-icons]]
      [:div.col-md-10.col-sm-10.col-xs-8  [:strong "HAPPY MOMSs"]]
      [:div.col-md-1.col-sm-1.col-xs-2.pull-right
       [:span.glyphicon.glyphicon-map-marker.moms-icons]]]]]])

(declare moms-search)
(rum/defc moms-search
  []
  [:div.input-group.moms-search
   [:input.form-control {:type "text"
                         :placeholder "Search for moms-friendly places....."}]
   [:span.input-group-btn
    [:button.btn.btn-default {:type "button"}
     [:span.glyphicon.glyphicon-search]]]])


(declare filter-title)
(rum/defc filter-title
  []
  [:div.title
   [:div#filter "FILTER"]]);} "FILTER"}]])



(declare view-all)
(rum/defc view-all < rum/reactive
  []
  (rum/react viewall)
  (let [on-close-fn #(reset! viewall false)]
    (when @viewall
      [:div.overlay
        [:div.col-md-4.col-md-offset-4.col-xs-12.col-sm-8.col-sm-offset-2
         [:div.moms-popup
           [:div.moms-header "Select Facilities"
            [:a.close {:on-click on-close-fn} "x"]]
           [:div.popup-content
             [:div.col-md-3.col-xs-4.moms-popup-fas {:on-click #(println "1")}
               [:img.facilities-icon {:style {:background-image "url(../images/icons/Dog.png)"}}]
               [:div "Pets Friendly"]]
             [:div.col-md-3.col-xs-4.moms-popup-fas {:on-click #(println "2")}
               [:img.facilities-icon {:style {:background-image "url(../images/icons/Dog.png)"}}]
               [:div "Pets"]]
             [:div.col-md-3.col-xs-4.moms-popup-fas {:on-click #(println "3")}
               [:img.facilities-icon {:style {:background-image "url(../images/icons/Dog.png)"}}]
               [:div "Feeding Chair"]]
             [:div.col-md-3.col-xs-4.moms-popup-fas {:on-click #(println "4")}
               [:img.facilities-icon {:style {:background-image "url(../images/icons/Dog.png)"}}]
               [:div "Pets"]]
             [:div.col-md-3.col-xs-4.moms-popup-fas {:on-click #(println "5")}
               [:img.facilities-icon {:style {:background-image "url(../images/icons/Dog.png)"}}]
               [:div "Pets Friendly"]]]


            ;[:div
            ; [:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Dog.png)"}}]
            ;  [:span "Pets Friendly"]]
            ; [:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Crib.png)"}}]
            ;  [:span "Kids Beds"]]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Survival-Bag.png)"}}]
            ;  [:span "Meds"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Toilet-Bowl.png)"}}]
            ;  [:span "Kids Toilet"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Chair.png)"}}]
            ;  [:span "Feeding Chair"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Teddy-Bear.png)"}}]
            ;  [:span "Play Area"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Charcoal.png)"}}]
            ;  [:spam "Take Away"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Controller.png)"}}]
            ;  [:span "Video Games"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Coffee-to-Go.png)"}}]
            ;  [:span "Take Away"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Paint-Palette.png)"}}]
            ;  [:span "Art Stuff"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Crying-Baby.png)"}}]
            ;  [:span "Baby Siter"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Party-Baloons.png)"}}]
            ;  [:span "Kids Parties"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Parking.png)"}}]
            ;  [:span "Parking"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Playground.png)"}}]
            ;  [:span "Outdoor Playground"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Porridge.png)"}}]
            ;  [:span "Kids Menu"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Rabbit.png)"}}]
            ;  [:span "Mini Zoo"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Sandbox.png)"}}]
            ;  [:span "Playarea"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Santa.png)"}}]
            ;  [:span "Animators"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Vegan-Food.png)"}}]
            ;  [:span "Vegetarian Food"]]
            ;[:div.col-xs-2.moms-fas-details
            ;  [:button.facilities-icon {:style {:background-image "url(../images/icons/Sofa.png)"}}]
            ;  [:span "Sofas"]]]
           [:div.moms-footer
             [:button.btn.btn-success
              {:type     "submit"
               :on-click on-close-fn}
              "Select"]]]]])))




(declare moms-filter)
(rum/defc moms-filter
  []
  [:div.container-fluid.moms-filter
   [:div.sub-title  "Kids Facilities"
    [:a.links.pull-right {:on-click #(do (reset! viewall true)
                                         (println @viewall))}
     "view all...."]]

   [:div
    [:button.facilities-icon {:style {:background-image "url(../images/icons/Dog.png)"}}]
    [:button.facilities-icon {:style {:background-image "url(../images/icons/Crib.png)"}}]
    [:button.facilities-icon {:style {:background-image "url(../images/icons/Survival-Bag.png)"}}]
    [:button.facilities-icon {:style {:background-image "url(../images/icons/Toilet-Bowl.png)"}}]
    [:button.facilities-icon {:style {:background-image "url(../images/icons/Chair.png)"}}]
    [:button.facilities-icon {:style {:background-image "url(../images/icons/Teddy-Bear.png)"}}]
    [:button.facilities-icon {:style {:background-image "url(../images/icons/Charcoal.png)"}}]
    [:button.facilities-icon {:style {:background-image "url(../images/icons/Controller.png)"}}]]])
    ;[:button.facilities-icon {:style {:background-image "url(../images/icons/Coffee-to-Go.png)"}}]]])
    ;[:button.facilities-icon {:style {:background-image "url(../images/icons/Paint-Palette.png)"}}]
    ;[:button.facilities-icon {:style {:background-image "url(../images/icons/Crying-Baby.png)"}}]
    ;[:button.facilities-icon {:style {:background-image "url(../images/icons/Party-Baloons.png)"}}]
    ;[:button.facilities-icon {:style {:background-image "url(../images/icons/Parking.png)"}}]
    ;[:button.facilities-icon {:style {:background-image "url(../images/icons/Playground.png)"}}]
    ;[:button.facilities-icon {:style {:background-image "url(../images/icons/Porridge.png)"}}]
    ;[:button.facilities-icon {:style {:background-image "url(../images/icons/Rabbit.png)"}}]
    ;[:button.facilities-icon {:style {:background-image "url(../images/icons/Sandbox.png)"}}]
    ;[:button.facilities-icon {:style {:background-image "url(../images/icons/Santa.png)"}}]
    ;[:button.facilities-icon {:style {:background-image "url(../images/icons/Vegan-Food.png)"}}]
    ;[:button.facilities-icon {:style {:background-image "url(../images/icons/Sofa.png)"}}]]




(declare moms-rating)
(rum/defc moms-rating
  []
  [:div.container-fluid.moms-rating
   [:div.col-xs-7
    [:div.sub-title "Mom's Rating"]
    [:div
     [:button.facilities-icon.icon-smaller {:style {:background-image "url(../images/icons/Baby.png)"}}]
     [:button.facilities-icon.icon-smaller {:style {:background-image "url(../images/icons/Baby.png)"}}]
     [:button.facilities-icon.icon-smaller {:style {:background-image "url(../images/icons/Baby.png)"}}]]]
   [:div.col-xs-5
    [:div.sub-title "Price"]
    [:div
     [:button.facilities-icon.icon-smaller {:style {:background-image "url(../images/icons/US-Dollar.png)"}}]
     [:button.facilities-icon.icon-smaller {:style {:background-image "url(../images/icons/US-Dollar.png)"}}]]]])




(declare moms-page)
(rum/defc moms-page
  []
  [:div.moms
   (moms-header)
   [:div.col-md-4.col-md-offset-4.col-xs-12.col-sm-8.col-sm-offset-2
    [:div.row.moms-content
     (moms-search)
     (filter-title)
     (moms-filter)
     (moms-rating)
     (view-all)]]])

