(ns {{name}}.pages.cart.cart 
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent]
   [{{name}}.events.cart-set :refer [cart_set]]
   [{{name}}.apiurimaker :refer [image_uri_maker]]
   )
  )

(defn is-mobile? []
  (let [user-agent (.-userAgent (.-navigator js/window))
        mobile-regex #"(?i)android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini"]
    (boolean (re-find mobile-regex user-agent))))






(defn cart_page []
  (let [Image antd/Image
        MinusOutlined icons/MinusOutlined
        PlusOutlined icons/PlusOutlined
        CloseOutlined icons/CloseOutlined
        Skeleton antd/Skeleton
        SkeletonNode (.-Node Skeleton)
        SkeletonInput (.-Input Skeleton)
        SkeletonButton (.-Button Skeleton)
        cart (reagent/cursor app-state [:cart])
        cart_summary (reagent/cursor app-state [:cart_summary])
        cart_stock_issues (reagent/cursor app-state [:cart_stock_issues])
        ]
    (fn []
      (let [raw-cart-items @cart
            summary @cart_summary
            loading? (or (not (:cart_loaded? @app-state))
                         (not (:cart_summary_loaded? @app-state)))
            stock-issues (or @cart_stock_issues {:show? false :items #{}})
            parse-int (fn [value]
                        (cond
                          (number? value) value
                          (string? value) (let [parsed (js/parseInt value 10)]
                                            (when-not (js/isNaN parsed) parsed))
                          :else nil))
            cart-items (->> raw-cart-items
                            (filter (fn [item]
                                      (let [qty (parse-int (:quantity item))]
                                        (and (number? qty) (> qty 0)))))
                            vec)
            has-stock-issues? (some (fn [item]
                                      (let [stock (parse-int (:stock_quantity item))
                                            qty (parse-int (:quantity item))]
                                        (and (number? stock)
                                             (number? qty)
                                             (> qty stock))))
                                    cart-items)
            show-stock-banner? (and (:show? stock-issues) has-stock-issues?)
            total-old (:summ summary)
            total-new (or (:final_summ summary) total-old)
            show-old? (and total-old total-new (not= total-old total-new))
            pending-ids (or (:cart_pending_ids @app-state) #{})
            
            web-app (.-WebApp js/Telegram)
            platform (when web-app (.-platform web-app))
            is-tg-mobile? (contains? #{"ios" "android"} platform)
            is-mobile-device? (is-mobile?)
            is-mobile (or is-tg-mobile? is-mobile-device?)
            
            padding-top 87
            ]
        [:div {:style {:paddingTop (if is-mobile (+ padding-top 24) 24)
                       :paddingRight 20
                       :paddingBottom 36
                       :paddingLeft 20
                       
                       :background "#ffffff"}
               }
         [:div {:style {:fontSize 40
                        :fontWeight 400
                        :marginBottom 16}}
          "Ваш заказ"
          ]
         (if loading?
           [:div {:style {:display "flex"
                          :flexDirection "column"
                          :gap 16}}
            (for [idx (range 3)]
              ^{:key (str "cart-skel-" idx)}
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
                               :background "#f0f0f0"}}]
                [:div {:style {:height 12
                               :width "40%"
                               :borderRadius 4
                               :background "#f0f0f0"}}]]
               [:div {:style {:marginLeft "auto"
                              :width 56
                              :height 20
                              :borderRadius 4
                              :background "#f0f0f0"}}]])
            [:div {:style {:marginTop 8
                           :display "flex"
                           :justifyContent "flex-end"}}
             [:div {:style {:height 24
                            :width 120
                            :borderRadius 4
                            :background "#f0f0f0"}}]]
           ]
           [:<> 
            (when show-stock-banner?
           [:div {:style {:border "var(--border-hairline) solid var(--color-accent)"
                          :borderRadius 6
                          :padding "10px 12px"
                          :fontSize 13
                          :color "#111"
                          :background "#f4fbf8"
                          :lineHeight "18px"
                          :marginBottom 16}}
            "Некоторые товары превышают остаток. "
            [:span {:style {:color "var(--color-accent)"}}
             "Проверьте позиции и нажмите «Оставить X»."]])

         (if (empty? cart-items)
           [:div {:style {:fontSize 16
                          :color "#777"}}
            "Корзина пуста"
            ]
           [:div {:style {:display "flex"
                          :flexDirection "column"
                          :gap 16}}
            (for [item cart-items]
              (let [attrs (:product_attributes item)
                    image-src (-> attrs :images first)
                    name (:product_name attrs)
                    color (:color attrs)
                    size (:size attrs)
                    article (:product_id attrs)
                    quantity (parse-int (:quantity item))
                    raw-stock (:stock_quantity item)
                    stock-quantity (cond
                                    (number? raw-stock) raw-stock
                                    (string? raw-stock) (let [parsed (js/parseInt raw-stock 10)]
                                                          (when-not (js/isNaN parsed) parsed))
                                    :else nil)
                    overstocked? (and (number? stock-quantity)
                                      (> quantity stock-quantity))
                    final-summ (:final_summ item)
                    discount-final-summ (:discount_final_summ item)
                    pending? (contains? pending-ids (:product_id item))
                    ]
                (if pending?
                  ^{:key (:product_id item)}
                  [:div {:style {:border "var(--border-hairline) solid var(--color-border)"
                                 :borderRadius 6
                                 :padding "12px 14px"
                                 :display "flex"
                                 :gap 14
                                 :height (if (not (= final-summ discount-final-summ))
                                           155
                                           145
                                           )
                                 :alignItems "flex-start"}}
                   [:div {:style {:width 90
                                  :height 120
                                  :borderRadius 6
                                  :overflow "hidden"}}
                    [:> SkeletonNode
                     {:active true
                      :style {:width 90
                              :height 120}}
                     [:div {:style {:width "100%"
                                    :height "100%"}}]]
                    ]
                   [:div {:style {:flex 1
                                  :minWidth 0
                                  :display "flex"
                                  :flexDirection "column"
                                  :gap 8}}
                    [:> SkeletonInput {:active true
                                       :style {:width "70%"
                                               :height 16}}]
                    [:> SkeletonInput {:active true
                                       :style {:width "55%"
                                               :height 12}}]
                    [:> SkeletonInput {:active true
                                       :style {:width "50%"
                                               :height 12}}]
                    [:> SkeletonInput {:active true
                                       :style {:width "65%"
                                               :height 12}}]
                    [:div {:style {:display "flex"
                                   :alignItems "center"
                                   :justify-content "space-between"
                                   :marginTop 10
                                   :width "100%"}}
                     [:> SkeletonButton {:active true
                                         :style {:width 40
                                                 :height 20
                                                 }
                                         }
                      ] 
                     [:> SkeletonButton {:active true
                                         :style {:width 40
                                                 :height 20
                                                 }
                                         }
                      ] 
                     ]
                     ]
                     ]
                  ^{:key (:product_id item)}
                  [:div {:style {:border (if overstocked?
                                           "var(--border-hairline) solid var(--color-accent)"
                                           "var(--border-hairline) solid var(--color-border)")
                                 :borderRadius 6
                                 :padding "12px 14px"
                                 :display "flex"
                                 :gap 14
                                 :alignItems "flex-start"
                                 :position "relative"}
                         :onClick (fn []
                                    (.impactOccurred (.-HapticFeedback (.-WebApp js/Telegram)) "light")
                                    (set! (.-href (.-location js/window)) (str "#/product/" (:unit_id attrs) "/" (:product_id item)))
                                    (swap! app-state assoc :page :product)
                                    (js/window.scrollTo 0 0))}
                   [:button {:type "button"
                             :title "Удалить"
                             :aria-label "Удалить"
                             :onClick (fn [e]
                                        (.stopPropagation e)
                                        (cart_set (:product_id item) 0))
                             :style {:position "absolute"
                                     :top 8
                                     :right 8
                                     :width 24
                                     :height 24
                                     :borderRadius "50%"
                                     :border "var(--border-hairline) solid var(--color-border)"
                                     :background "#ffffff"
                                     :color "var(--color-accent)"
                                     :display "flex"
                                     :alignItems "center"
                                     :justifyContent "center"
                                     :padding 0
                                     :cursor "pointer"}}
                    [:> CloseOutlined {:style {:fontSize 12}}]]
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
                    
                  
                    (if (and overstocked? (pos? stock-quantity))
                      [:div {:style {:marginTop 8
                                     :display "flex"
                                     :alignItems "center"
                                     :gap 8
                                     :flexWrap "wrap"}}
                       [:span {:style {:fontSize 12
                                       :color "var(--color-accent)"}}
                        (str "В наличии осталось " stock-quantity " шт.")]
                       [:button {:type "button"
                                 :onClick (fn [e]
                                            (.stopPropagation e)
                                            (cart_set (:product_id item) stock-quantity))
                                 :style {:border "none"
                                         :background "var(--color-accent)"
                                         :color "#ffffff"
                                         :borderRadius 10
                                         :padding "4px 8px"
                                         :fontSize 11
                                         :fontWeight 500
                                         :cursor "pointer"}}
                        (str "Оставить " stock-quantity)]]
                      
                  
                    [:div {:style {:display "flex"
                                   :alignItems "center"
                                   :gap 8
                                   :marginTop 10
                                   :width "100%"}
                           :onClick (fn [e] (.stopPropagation e))}
                       [:button {:type "button"
                                 :onClick (fn [e]
                                            (.stopPropagation e)
                                            (cart_set (:product_id item) (dec quantity)))
                                 :style {:width 20
                                         :height 20
                                         :borderRadius "50%"
                                         :border "var(--border-hairline) solid var(--color-border)"
                                         :background "transparent"
                                         :color "var(--color-accent)"
                                         :display "flex"
                                         :alignItems "center"
                                         :justifyContent "center"
                                         :padding 0
                                         :cursor "pointer"}}
                        [:> MinusOutlined {:style {:fontSize 12}}]]
                       [:span {:style {:fontSize 14
                                       :color "var(--color-accent)"
                                       :minWidth 16
                                       :textAlign "center"}}
                        quantity]
                       [:button {:type "button"
                                 :onClick (fn [e]
                                            (.stopPropagation e)
                                            (if-not (> (inc quantity) (:stock_quantity item))
                                              (cart_set (:product_id item) (inc quantity))))
                                 :style {:width 20
                                         :height 20
                                         :borderRadius "50%"
                                         :border "var(--border-hairline) solid var(--color-border)"
                                         :background "transparent"
                                         :color "var(--color-accent)"
                                         :display "flex"
                                         :alignItems "center"
                                         :justifyContent "center"
                                         :padding 0
                                         :cursor "pointer"}}
                        [:> PlusOutlined {:style {:fontSize 12}}]]
                       [:div {:style {:marginLeft "auto"
                                      :display "flex"
                                      :flexDirection "column"
                                      :alignItems "flex-end"
                                      :gap 2
                                      :whiteSpace "nowrap"}}
                        (when (not (= final-summ discount-final-summ))
                          [:div {:className "price-strike"
                                 :style {:fontSize 11
                                         :color "#5f5f5f"
                                         :lineHeight "14px"}}
                           (str final-summ "₽")])
                        [:div {:style {:fontSize 18
                                       :color "var(--color-accent)"
                                       :fontWeight 400
                                       :lineHeight "20px"}}
                         (str (or discount-final-summ final-summ) "₽")
                         ]
                        ]
                       ]
                      
                      ) 
                     ]
                     ]
                )
                   )
                   )
                   ]
                   )
                     
                     
                     
                     (if-not (empty? cart-items)
                       [:div {:style {:marginTop 24
                                      :display "flex"
                                      :flexDirection "column"
                                      :alignItems "flex-end"
                                      :gap 6
                                      :width "100%"}}
                        (when show-old?
                          [:div {:className "price-strike"
                                 :style {:fontSize 14
                                         :color "#5f5f5f"
                                         :textAlign "right"}}
                           (str total-old "₽")])
                        [:div {:style {:fontSize 24
                                       :fontWeight 500
                                       :color "#111"
                                       :textAlign "right"}}
                         "Итог: "
                         [:span {:style {:color "var(--color-accent)"}} (str total-new "₽")]
                         ]
                        ]
                       )
                     
                     [:div {:style {:border "var(--border-hairline) solid var(--color-border)"
                                    :borderRadius 6
                                    :padding "12px 16px"
                                    :fontSize 13
                                    :textAlign "center"
                                    :color "#666"
                                    :lineHeight "18px"
                                    :marginTop 24}}
                      "Оформляя заказ вы подтверждаете, что ознакомлены с условиями "
                      [:span {:style {:color "var(--color-accent)"
                                      :cursor "pointer"
                                      :textDecoration "underline"}
                              :onClick (fn []
                                         (swap! app-state assoc :page :information)
                                         (set! (.-href (.-location js/window)) "#/information"))}
                       "публичной оферты"
                       ]
                      ]
                     ]
           )
           ]
           ) 
           ) 
           ) 
           )
