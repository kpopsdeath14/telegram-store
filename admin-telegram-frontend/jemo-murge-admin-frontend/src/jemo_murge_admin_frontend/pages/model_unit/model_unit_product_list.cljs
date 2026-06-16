(ns jemo-murge-admin-frontend.pages.model-unit.model-unit-product-list
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [clojure.string :as str]
   [reagent.core :as reagent :refer [as-element]]
   [jemo-murge-admin-frontend.api-uri-maker :refer [image_uri_maker]]
   ))




(defn model_unit_product_list []
  (let [List antd/List
        Image antd/Image
        Checkbox antd/Checkbox
        Skeleton antd/Skeleton
        SkeletonNode (.-Node Skeleton)

        model_unit (reagent/cursor app-state [:model_unit])
        selected_units (reagent/cursor app-state [:selected_units_model_unit])
        products_mode (reagent/cursor app-state [:products_mode])
        second-lvl-config (reagent/cursor app-state [:second_lvl_config])
        second-level-common-attributes (reagent/cursor app-state [:second_level_common_attributes])
        
        config-attributes-reaction (reagent/reaction
                                    (mapv keyword
                                          (mapv :attribute_name
                                                (:modification_attributes @second-lvl-config))))
        ]
    (fn []
      (let [config-attributes @config-attributes-reaction
            Message (.-message antd)
            notify-copy (fn []
                          (when Message
                            (.success Message "ID скопирован")))
            copy-to-clipboard (fn [text]
                                (if (and (exists? js/navigator)
                                         (.-clipboard js/navigator))
                                  (-> (.writeText (.-clipboard js/navigator) text)
                                      (.then (fn [] (notify-copy)))
                                      (.catch (fn [_]
                                                (let [textarea (.createElement js/document "textarea")]
                                                  (set! (.-value textarea) text)
                                                  (.appendChild (.-body js/document) textarea)
                                                  (.select textarea)
                                                  (.execCommand js/document "copy")
                                                  (.remove textarea)
                                                  (notify-copy)))))
                                  (let [textarea (.createElement js/document "textarea")]
                                    (set! (.-value textarea) text)
                                    (.appendChild (.-body js/document) textarea)
                                    (.select textarea)
                                    (.execCommand js/document "copy")
                                    (.remove textarea)
                                    (notify-copy))))
            ] 
        [:> List {:style {:margin-top 25
                          :margin-bottom 25}
                  :header (as-element [:div {:style {:color "white" :height 0}} (count @selected_units)])
                  :locale {:emptyText "таких товаров нет"}
                  :dataSource @model_unit
                  :renderItem (fn [product]
                                (let [product (js->clj product :keywordize-keys true)
                                      product_id (product :product_id)
                                      checked? (reagent/reaction (some #(= % product_id) @selected_units)) 
                                      common-attributes (dissoc (select-keys product config-attributes) :images)]
                                  (as-element
                                   (if (= @model_unit [{} {} {} {}])
                                     [:div {:style {:height 110
                                                    :borderRadius 15
                                                    :marginBottom 25
                                                    :box-shadow "0 2px 8px rgba(0, 2, 5, 0.25)"}}
                                      [:> SkeletonNode
                                       {:style {:height "100%"
                                                :width "100%"}
                                        :active true}]]
                                     
                                     
                                     [:div {:style {:backgroundColor (if @checked?
                                                                       (case @products_mode
                                                                         "catalog" "#D9D9D9"
                                                                         "archive" "#8E8E8E")
                                                                       (case @products_mode
                                                                         "catalog" "white"
                                                                         "archive" "#D9D9D9"))
                                                    :box-sizing "border-box"
                                                    :overflow "hidden"
                                                    :flex-wrap "nowrap"
                                                    :padding "20px 30px"
                                                    :display "flex"
                                                    :height 110
                                                    :border-radius 15
                                                    :justify-content "space-between"
                                                    :align-items "center"
                                                    :marginBottom 25
                                                    :box-shadow "0 2px 8px rgba(0, 2, 5, 0.25)"}
                                            :onClick (fn [e]
                                                       (swap! app-state assoc :selected_units_catalog [])
                                                       (js/window.scrollTo 0 0)
                                                       (set! (.-href (.-location js/window)) (str "#/product/" (js/encodeURIComponent product_id))))}
                                      
                                      [:div {:style {:display "flex"
                                                     :gap 20
                                                     :align-items "center"}}
                                       [:> Image {:preview false
                                                  :src (image_uri_maker (first (product :images)))
                                                  :style {:height "70px"
                                                          :width "70px"
                                                          :margin-left "0px"
                                                          :border-radius "15px"
                                                          :object-fit "cover"}}]
                                       
                                       
                                       [:div {:style {:display "flex"
                                                      :align-items "center"
                                                      :gap 10}} 
                                        [:span {:style {:font-size 14
                                                        :font-weight 600
                                                        :font-family "monospace"
                                                        :padding "6px 12px"
                                                        :color "#111827"
                                                        :background "#F9FAFB"
                                                        :border "1px solid #E5E7EB"
                                                        :border-radius 999
                                                        :box-shadow "inset 0 1px 0 rgba(255,255,255,0.7)"
                                                        :cursor "pointer"}
                                                :title "Скопировать ID"
                                                :onClick (fn [e]
                                                           (.stopPropagation e)
                                                           (copy-to-clipboard (str product_id)))}
                                         product_id]]]
                                      
                                      
                                      
                                      [:div {:style {:display "flex"
                                                     :gap 10
                                                     :align-items "center"
                                                     :flex-wrap "wrap"}}
                                       
                                       (for [[attribute-name attribute-value] common-attributes]
                                         (let [value (cond
                                                       (vector? attribute-value) (str/join ", " attribute-value)
                                                       (nil? attribute-value) "-"
                                                       :else attribute-value)]
                                           [:div {:key (str attribute-name)
                                                  :style {:font-weight 500
                                                          :font-size 16
                                                          :display "flex"
                                                          :overflow "hidden"
                                                          :white-space "nowrap"
                                                          :text-overflow "ellipsis"
                                                          :align-items "center"
                                                          :justify-content "center"
                                                          :height 32
                                                          :max-width 220
                                                          :padding "0 12px"
                                                          :color "#1F2937"
                                                          :background "#F3F4F6"
                                                          :border "1px solid #E5E7EB"
                                                          :border-radius 999
                                                          :box-shadow "inset 0 1px 0 rgba(255,255,255,0.7)"}} 
                                            (str value)]))
                                       ]
                                      
                                      [:div {:style {:width 100
                                                     :display "flex"
                                                     :height "100%"
                                                     :justify-content "flex-end"}
                                             :onClick (fn [e] (.stopPropagation e))}
                                       
                                       [:> Checkbox {:checked @checked?
                                                     :className "large-checkbox"
                                                     :onChange (fn [e]
                                                                 (if @checked?
                                                                   (swap! app-state assoc :selected_units_model_unit (remove #(= % product_id) @selected_units))
                                                                   (swap! app-state assoc :selected_units_model_unit (conj @selected_units product_id))))}]]]))))}]
        )
      )
    )
  )
