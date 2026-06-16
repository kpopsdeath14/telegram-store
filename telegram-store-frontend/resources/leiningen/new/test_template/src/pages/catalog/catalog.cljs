(ns {{name}}.pages.catalog.catalog
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.pages.catalog.list :refer [catalog_list]]
   [{{name}}.pages.catalog.banner :refer [banner]]
   [{{name}}.pages.catalog.filters :refer [filters]]
   [{{name}}.pages.catalog.logo :refer [logo]]
   [{{name}}.pages.catalog.search :refer [search]]
   [{{name}}.pages.catalog.sections-menu :refer [scroll-menu]]
   )
  )



(defn catalog_page []
  (let [Card antd/Card
        List antd/List
        Button antd/Button
        ListItem (.-Item List)
        Image antd/Image
        Skeleton antd/Skeleton
        SkeletonNode (.-Node Skeleton)
        SkeletonInput (.-Input Skeleton)
        SkeletonButton (.-Button Skeleton)
        loading? (reagent/atom false)
        images-loaded (reagent/atom false)
        products (reagent/cursor app-state [:products])
        visible_count (reagent/cursor app-state [:visible_count])
        app-ready? (reagent/cursor app-state [:app_ready?])
        products-loaded? (reagent/cursor app-state [:products_loaded?])
        filters-loaded? (reagent/cursor app-state [:filters_loaded?])
        banners-loaded? (reagent/cursor app-state [:banners_loaded?])]

    (defn load-more []
      (reset! loading? true)
      (js/setTimeout
       (fn []
         (swap! app-state assoc :visible_count (+ 20 @visible_count))
         (reset! loading? false))
       300)
      )


    (fn []
      (reagent/after-render
       (fn []
         (if-not (nil? (js/document.getElementById (str (:current_unit_id @app-state) "_" (:current_product_id @app-state))))
           (let [element (js/document.getElementById (str (:current_unit_id @app-state) "_" (:current_product_id @app-state)))]
             (do
               (.scrollIntoView element #js {:block "center"}) 
               )
             (swap! app-state assoc :current_product_id nil)
             (swap! app-state assoc :current_unit_id nil)
             )
           )
         )
       )
      (let [catalog-loading? (or (not @app-ready?)
                                 (not @products-loaded?)
                                 (not @filters-loaded?)
                                 (not @banners-loaded?))
            banner-height 300
            skeleton-banner
            [:div {:style {:width "100%"
                           :height banner-height
                           :overflow "hidden"}}
             [:> SkeletonNode
              {:active true
               :style {:width "100vw"
                       :height "100vw"}}
              [:div {:style {:width "100%"
                             :height banner-height}}
               ]
              ]
             ]
            
            skeleton-catalog-list
            [:> List {:style {:margin "16px 0px"
                              :padding 0}
                      :dataSource (range 4)
                      :grid {:column 2
                             :gutter 14}
                      :renderItem (fn [_]
                                    (as-element
                                     [:> ListItem
                                     [:div {:style {:width "100%"
                                                    :background "white"
                                                    :border "var(--border-hairline) solid var(--color-border)"
                                                    :border-radius 4
                                                    :overflow "hidden"
                                                    :box-shadow "none"}}
                                       [:div {:style {:position "relative"
                                                      :width "100%"
                                                      :aspect-ratio "1 / 1"
                                                      :display "flex"
                                                      :justify-content "center"
                                                      :align-items "center"
                                                      :overflow "hidden"
                                                      :border-radius 4}}
                                        
                                        [:div {:style {:width "100%"
                                                       :height "100%"}
                                               }
                                         [:> SkeletonNode
                                          {:active true
                                           :style {:height "100vw"
                                                   :width "100vw"}}
                                          ]
                                         ] 
                                        ]
                                       [:div {:style {:padding "10px 18px 16px 18px"}}
                                        [:div {:style {:display "flex"
                                                       :gap "6px"
                                                       :align-items "baseline"}}
                                         [:div {:style {:height 22
                                                        :width 72
                                                        :border-radius 4
                                                        :background "#f0f0f0"}}]]
                                        [:div {:style {:margin-top 4
                                                       :height 18
                                                       :width "75%"
                                                       :border-radius 4
                                                       :background "#f0f0f0"}}]
                                        [:div {:style {:margin-top 6
                                                       :height 18
                                                       :width "55%"
                                                       :border-radius 4
                                                       :background "#f0f0f0"}}]]]]))}
                                                       ]
                                                       ]
        [:div {:style {:background "#ffffff"}}
         [logo]
         (if catalog-loading?
           skeleton-banner
           [banner]
           )
         
         [:div {:style {:padding "10px 28px 16px"}} 
          (if catalog-loading?
            [:<>
             [:div {:style {:display "flex"
                            :width "100%"
                            :gap "10px"
                            :align-items "center"}}
              [:div {:style {:flex 1}}
               [:> SkeletonInput
                {:active true
                 :block true
                 :style {:height 40
                         :borderRadius 20}}]]
              ]
             [:div {:style {:display "flex"
                            :align-items "center"
                            :gap "8px"
                            :margin-top "10px"}
                    }
              [:> SkeletonNode
               {:active true
                :style {:width 32
                        :height 32
                        :borderRadius 16}}]
              [:> SkeletonNode
               {:active true
                :style {:width 32
                        :height 32
                        :borderRadius 16}}]
              [:div {:style {:display "flex"
                             :overflow-x "auto"
                             :gap "14px"
                             :scrollbar-width "none"
                             :-ms-overflow-style "none"
                             }
                     }
               [:> SkeletonInput
                {:active true
                 :block true
                 :style {:height 16
                         :borderRadius 6}}]
               [:> SkeletonInput
                {:active true
                 :block true
                 :style {:height 16
                         :width "80%"
                         :borderRadius 6}}
                ]
               ]
              ]
             skeleton-catalog-list
             
             ]
            [:<>
             [search]
             [:div {:style {:display "flex"
                            :align-items "center"
                            :gap "12px"
                            :margin-top "10px"}}
              [filters]
              [:div {:style {:flex 1}}
               [scroll-menu]]]
             [catalog_list]])
          ] 
         ])
      )
    )
    )



(comment
  [:> SkeletonButton
   {:active true
    :shape "circle"
    :style {:width 40
            :height 40}}]
  )