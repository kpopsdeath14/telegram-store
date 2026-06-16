(ns {{name}}.pages.favorites.favorites
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent]
   [{{name}}.events.favorite-del :refer [favorite_del]]
   [{{name}}.apiurimaker :refer [image_uri_maker]]
   [{{name}}.pages.catalog.logo :refer [logo]]
   [{{name}}.components.icons :refer [heart-icon]]
   )
  )



(defn favorites_page []
  (let [Image antd/Image
        favorites (reagent/cursor app-state [:favorites])
        favorites_loaded? (reagent/cursor app-state [:favorites_loaded?])
        ]
    (fn []
      (let [items @favorites
            loading? (not @favorites_loaded?)
            web-app (.-WebApp js/Telegram)
            ]
        [:div {:style {:background "#ffffff"}}
         [logo]
         [:div {:style {:paddingTop 24
                        :paddingRight 20
                        :paddingBottom 36
                        :paddingLeft 20
                        }
                }
          [:div {:style {:fontSize 40
                         :fontWeight 400
                         :marginBottom 16}}
           "Избранное"]
         
          (if loading?
            [:div {:style {:display "flex"
                           :flexDirection "column"
                           :gap 16}}
             (for [idx (range 3)]
               ^{:key (str "fav-skel-" idx)}
               [:div {:style {:border "var(--border-hairline) solid #e5e5e5"
                              :borderRadius 6
                              :padding "12px 14px"
                              :display "flex"
                              :gap 14
                              :alignItems "flex-start"}}
                [:div {:style {:width 90
                               :height 120
                               :borderRadius 6
                               :background "#f0f0f0"}}]
                [:div {:style {:flex 1
                               :display "flex"
                               :flexDirection "column"
                               :gap 8}}
                 [:div {:style {:height 16
                                :width "70%"
                                :borderRadius 4
                                :background "#f0f0f0"}}]
                 [:div {:style {:height 12
                                :width "45%"
                                :borderRadius 4
                                :background "#f0f0f0"}}]
                 [:div {:style {:height 12
                                :width "35%"
                                :borderRadius 4
                                :background "#f0f0f0"}}]]])]
            (if (empty? items)
              [:div {:style {:fontSize 16
                             :color "#777"}}
               "Пока нет избранных товаров"]
              [:div {:style {:display "flex"
                             :flexDirection "column"
                             :gap 16}}
               (for [item items]
                 (let [attrs (or (:product_attributes item) item)
                       product-id (or (:product_id item) (:product_id attrs))
                       unit-id (or (:unit_id item) (:unit_id attrs))
                       name (:product_name attrs)
                       color (:color attrs)
                       size (:size attrs)
                       article (:product_id attrs)
                       image-src (-> attrs :images first)]
                   ^{:key (str "fav-" product-id)}
                   [:div {:style {:border "var(--border-hairline) solid var(--color-border)"
                                  :borderRadius 6
                                  :padding "12px 14px"
                                  :display "flex"
                                  :gap 14
                                  :alignItems "flex-start"}
                          :onClick (fn []
                                     (.impactOccurred (.-HapticFeedback (.-WebApp js/Telegram)) "light")
                                     (set! (.-href (.-location js/window)) (str "#/product/" unit-id "/" product-id)) 
                                     (js/window.scrollTo 0 0))}
                    [:> Image {:src (image_uri_maker image-src)
                               :preview false
                               :style {:width 90
                                       :height 120
                                       :borderRadius 6
                                       :objectFit "cover"}}]
                    [:div {:style {:flex 1
                                   :minWidth 0
                                   :display "flex"
                                   :flexDirection "column"}}
                     [:div {:style {:fontSize 16
                                    :fontWeight 400
                                    :color "#111"
                                    :lineHeight "20px"
                                    :wordBreak "break-word"}}
                      name]
                     [:div {:style {:fontSize 11
                                    :marginTop 12
                                    :color "#9a9a9a"
                                    :lineHeight "13px"}}
                      (str "Цвет: " color)]
                     [:div {:style {:fontSize 11
                                    :color "#9a9a9a"
                                    :lineHeight "13px"}}
                      (str "Размер: " size)]
                     [:div {:style {:fontSize 11
                                    :color "#9a9a9a"
                                    :lineHeight "13px"
                                    :wordBreak "break-word"}}
                      (str "Артикул: " article)]
                     ]
                    [:div {:style {:marginLeft "auto"
                                   :display "flex"
                                   :alignItems "flex-start"}
                           :onClick (fn [e] (.stopPropagation e))}
                     [:button {:type "button"
                               :onClick (fn [e]
                                          (.impactOccurred (.-HapticFeedback (.-WebApp js/Telegram)) "light")
                                          (.stopPropagation e)
                                          (favorite_del product-id nil))
                               :style {:border "none"
                                       :background "transparent"
                                       :padding 0
                                       :cursor "pointer"}}
                      [heart-icon {:filled? true
                                   :size 22}]]]]))]))]
         ] 
        ) 
      ) 
    ) 
    )
