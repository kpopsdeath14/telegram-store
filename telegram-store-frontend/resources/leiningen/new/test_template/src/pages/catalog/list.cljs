(ns {{name}}.pages.catalog.list
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.apiurimaker :refer [image_uri_maker]]
   [{{name}}.events.favorite-add :refer [favorite_add]]
   [{{name}}.events.favorite-del :refer [favorite_del]]
   [{{name}}.components.icons :refer [heart-icon]]
   )
  )



(defonce catalog-image-loaded (reagent/atom #{}))

(defn catalog_list []
  (let [Card antd/Card
        List antd/List
        ListItem (.-Item List)
        Image antd/Image

         products (reagent/cursor app-state [:products])
         favorites (reagent/cursor app-state [:favorites])
         ]
    (fn []
      (let [_ @catalog-image-loaded
            favorite-ids (into #{}
                               (keep (fn [item]
                                       (or (:product_id item)
                                           (get-in item [:product_attributes :product_id])))
                                     @favorites))]
        (if (= @products [:empty])
          nil
          [:> List {:style {:margin "16px 0px"
                          :padding 0}
                  :dataSource @products
                  :rowKey (fn [item]
                            (let [info (js->clj item :keywordize-keys true)]
                              (str (:unit_id info) "-" (:product_id info))))
                  :grid {:column 2
                         :gutter 14}
                  :renderItem (fn [product]
                (let [product_info (js->clj product :keywordize-keys true)
                                      unit-id (:unit_id product_info)
                                      product-id (:product_id product_info)
                                      product-key (str unit-id "-" product-id)
                                      scroll-id (str unit-id "_" product-id)
                                      image-src (image_uri_maker (-> product_info :images first))
                                      image-ready? (contains? @catalog-image-loaded product-key)
                                      favored? (contains? favorite-ids product-id)
                                      ]
                  (as-element
                   ^{:key product-key}
                   [:> ListItem {:id scroll-id}
                                  [:> Card {:style {:width "100%"
                                                    :background "white"
                                                    :border "var(--border-hairline) solid var(--color-border)"
                                                    :border-radius 4
                                                    :overflow "hidden"
                                                    :box-shadow "none"}
                                            :onClick (fn []
                                                       (.impactOccurred (.-HapticFeedback (.-WebApp js/Telegram)) "light")
                                                       (set! (.-href (.-location js/window)) (str "#/product/" unit-id "/" product-id))
                                                       (js/window.scrollTo 0 0)
                                                       )
                                            :cover (as-element
                                                    [:div {:style {:position "relative"
                                                                   :width "100%"
                                                                   :aspect-ratio "1 / 1" 
                                                                   :display "flex" 
                                                                   :justify-content "center" 
                                                                   :align-items "center" 
                                                                   :overflow "hidden"
                                                                   :border-radius "4px 4px 0 0"
                                                                   :box-sizing "border-box" 
                                                                   :background "white"}}
                                                     [:> Image
                                                      {:style {:width "100%"
                                                               :height "100%"
                                                               :object-fit "cover"
                                                               :object-position "center"
                                                               :display "block"
                                                               :filter (if image-ready?
                                                                         "none"
                                                                         "blur(5px)")
                                                               :transition "filter 200ms ease"} 
                                                       :preview false
                                                       :src image-src
                                                       :onLoad (fn []
                                                                 (swap! catalog-image-loaded conj product-key))
                                                       :onError (fn []
                                                                  (swap! catalog-image-loaded conj product-key))}]

                                                     [:button {:type "button"
                                                               :onClick (fn [e]
                                                                          (.impactOccurred (.-HapticFeedback (.-WebApp js/Telegram)) "light")
                                                                          (.stopPropagation e)
                                                                          (if favored?
                                                                            (favorite_del product-id nil)
                                                                            (favorite_add product-id nil)))
                                                               :style {:position "absolute"
                                                                       :top 8
                                                                       :right 8
                                                                       :width 26
                                                                       :height 26
                                                                       :border "none"
                                                                       :background "transparent"
                                                                       :padding 0
                                                                       :cursor "pointer"}}
                                                      [heart-icon {:filled? favored?
                                                                   :size 22}]]

                                                     (if-not (nil? (:tags product_info))
                                                       [:div {:style {:border-radius "20px"
                                                                      :background "rgba(255,255,255,0.95)"
                                                                      :backdrop-filter "blur(4px)"
                                                                      :position "absolute"
                                                                      :top 8
                                                                      :left 8
                                                                      :display "flex"
                                                                      :justify-content "center"
                                                                      :align-items "center"
                                                                      :padding "4px 12px"
                                                                      :font-size "12px"
                                                                      :font-weight 500
                                                                      :color "#1f1f1f"
                                                                      :border "1px solid #595958"}}
                                                        (first (:tags product_info))])])}

                                   [:div {:style {:padding "10px 18px 16px 18px" 
                                                  :border-top "1px solid rgba(89, 89, 88, 0.2)"
                                                  }}
                                    (let [prices (:prices product_info)
                                          telegram_price (->> prices (filter :telegram_price) first :telegram_price)
                                          discount (->> prices (filter :discount_price) first :discount_price)]
                                      [:div {:style {:display "flex"
                                                     :gap "6px"
                                                     :align-items "center"}}
                                       (if discount
                                         [:<>
                                          [:span {:style {:font-size "17px"
                                                          :font-weight 100
                                                          :line-height "100%"
                                                          :color "var(--color-accent)"}}
                                           (:price discount) "₽"]
                                          [:span {:className "price-strike"
                                                  :style {:font-size "10px"
                                                          :font-weight 100
                                                          :line-height "100%"
                                                          :color "#8c8c8c"}}
                                           (:price telegram_price) "₽"]
                                          ]
                                         [:span {:style {:font-size "17px"
                                                         :font-weight 100
                                                         :line-height "100%"
                                                         :color "var(--color-accent)"}}
                                          (:price telegram_price) "₽"])])

                                    [:div {:style {:margin-top 4
                                                   :font-size "14.5px"
                                                   :font-weight 400
                                                   :color "#1f1f1f"
                                                   :line-height 1.4
                                                   :overflow "hidden"
                                                   :display "-webkit-box"
                                                   :WebkitLineClamp 2
                                                   :WebkitBoxOrient "vertical"}}
                                     (:product_name product_info)
                                     ]
                                    ]
                                    ]
                                    ]
                                    )
                                    )
                                    )
                                    }
                                    ]
                                    )
                                    )
                                    )
                                    )
                                    )

