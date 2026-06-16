(ns jemo-murge-admin-frontend.pages.units.list
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [jemo-murge-admin-frontend.api-uri-maker :refer [image_uri_maker]]
   )
  )



(defn units_list []
  (let [List antd/List
        Image antd/Image
        Checkbox antd/Checkbox
        Skeleton antd/Skeleton
        SkeletonNode (.-Node Skeleton)

        units (reagent/cursor app-state [:units])
        selected_units_catalog (reagent/cursor app-state [:selected_units_catalog])
        search_value (reagent/cursor app-state [:model_unit_search_value])
        filters_picked (reagent/cursor app-state [:filters_picked])
        products_mode (reagent/cursor app-state [:products_mode])

        first-lvl-config (reagent/cursor app-state [:first_lvl_config])

        config-attributes-reaction (reagent/reaction
                                    (mapv keyword
                                          (mapv :attribute_name
                                                (:common_attributes @first-lvl-config))))]

    (fn []
      (let [config-attributes @config-attributes-reaction]

        [:> List {:style {:margin-top 25
                          :margin-bottom 25}
                  :locale {:emptyText "таких товаров нет"}
                  :header (as-element [:div {:style {:color "white" :height 0}} (count @selected_units_catalog)])
                  :dataSource @units
                  :renderItem (fn [product]
                                (let [product (js->clj product :keywordize-keys true)
                                      unit_id (:unit_id product)
                                      checked? (reagent/reaction (some #(= % unit_id) @selected_units_catalog))

                                      ;; Используем config-attributes из внешнего let
                                      common-attributes (dissoc (select-keys product config-attributes) :images)
                                      ]
                                  (as-element
                                   (if (= @units [{} {} {} {}])
                                     [:div {:style {:height 110
                                                    :borderRadius 15
                                                    :marginBottom 25
                                                    :box-shadow "0 2px 8px rgba(0, 2, 5, 0.25)"}}
                                      [:> SkeletonNode
                                       {:style {:height 110
                                                :width "100%"}
                                        :active true}]]

                                     [:div {:style {:backgroundColor (if @checked?
                                                                       (case @products_mode
                                                                         "catalog" "#D9D9D9"
                                                                         "archive" "#8E8E8E")
                                                                       (case @products_mode
                                                                         "catalog" "white"
                                                                         "archive" "#D9D9D9"))
                                                    :boxSizing "border-box"
                                                    :padding "20px 30px"
                                                    :display "flex"
                                                    :height 110
                                                    :overflow "hidden"
                                                    :flex-wrap "nowrap"
                                                    :borderRadius 15
                                                    :justifyContent "space-between"
                                                    :alignItems "center"
                                                    :marginBottom 25
                                                    :box-shadow "0 2px 8px rgba(0, 2, 5, 0.25)"}
                                            :onClick (fn [e]
                                                       (swap! app-state assoc :selected_units_catalog [])
                                                       (js/window.scrollTo 0 0)
                                                       (set! (.-href (.-location js/window)) (str "#/model-unit/" (js/encodeURIComponent unit_id))))}

                                      [:div {:style {:display "flex"
                                                     :gap 50
                                                     :alignItems "center"}}
                                       [:> Image {:preview false
                                                  :src (image_uri_maker (first (product :images)))
                                                  :style {:height "70px"
                                                          :width "70px"
                                                          :marginLeft "0px"
                                                          :borderRadius "8px"
                                                          :objectFit "cover"}}]

                                       [:div {:style {:display "flex"
                                                      :flexDirection "column"
                                                      :gap 4
                                                      :maxWidth 320}}
                                        [:span {:style {:fontSize 11
                                                        :fontWeight 600
                                                        :letterSpacing "0.08em"
                                                        :textTransform "uppercase"
                                                        :color "#9CA3AF"}}
                                         "Товар"]
                                        [:span {:style {:fontSize 20
                                                        :fontWeight 700
                                                        :color "#111827"
                                                        :lineHeight 1.2
                                                        :whiteSpace "nowrap"
                                                        :overflow "hidden"
                                                        :textOverflow "ellipsis"}}
                                         (or (:product_name product) "Без названия")]]

                                       [:div {:style {:display "flex"
                                                      :gap 20
                                                      :align-items "center"}}

                                        (for [attribute common-attributes]
                                          [:div {:style {:font-weight 300
                                                         :font-size 24
                                                         :display "flex"
                                                         :overflow "hidden"
                                                         :white-space "nowrap"
                                                         :align-items "center"
                                                         :justify-content "center"
                                                         :height 45
                                                         :padding 10
                                                         :border "1px solid black"
                                                         :border-radius 6}}
                                           attribute])]]



                                      [:div {:style {:width 100
                                                     :display "flex"
                                                     :height "100%"
                                                     :justify-content "flex-end"}
                                             :onClick (fn [e] (.stopPropagation e))}

                                       [:> Checkbox {:checked @checked?
                                                     :className "large-checkbox"
                                                     :onChange (fn [e]
                                                                 (if @checked?
                                                                   (swap! app-state assoc :selected_units_catalog (remove #(= % unit_id) @selected_units_catalog))
                                                                   (swap! app-state assoc :selected_units_catalog (conj @selected_units_catalog unit_id))))}]]]))))}]))))
