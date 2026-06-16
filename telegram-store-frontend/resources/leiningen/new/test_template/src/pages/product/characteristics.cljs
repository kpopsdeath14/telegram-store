(ns {{name}}.pages.product.characteristics
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   ["@ant-design/icons" :as icons]
   ["react-photo-view" :as photo_review]
   [reagent.core :as reagent :refer [as-element]]
   [clojure.string :as str]
   [{{name}}.events.cart-set :refer [cart_set]]
   )
  )




(defn characteristics []
  (let [Space antd/Space
        Button antd/Button
        Skeleton antd/Skeleton

        PlusOutlined icons/PlusOutlined
        MinusOutlined icons/MinusOutlined

        unit (reagent/cursor app-state [:unit])
        cart (reagent/cursor app-state [:cart])
        desc-open? (reagent/atom true)
        fabric-open? (reagent/atom false)
        sizes-open? (reagent/atom false)]
    (fn []
      (let [product_current (reagent/cursor app-state [:product_current])
            selected-color (reagent/cursor app-state [:product_current :color])
            selected-size (reagent/cursor app-state [:product_current :size])

            all-colors (->> (:color @unit)
                            (map :color)
                            (remove nil?)
                            distinct)

            all-sizes (->> (:color @unit)
                           (mapcat :size)
                           (map :size)
                           (remove nil?)
                           distinct)

            filtered-sizes (if @selected-color
                             (->> (:color @unit)
                                  (filter #(= (:color %) @selected-color))
                                  (mapcat :size)
                                  (map :size)
                                  (remove nil?)
                                  distinct)
                             all-sizes)

            accent-color "var(--color-accent)"
            border-color "#595958"
            set-color! (fn [value]
                         (reset! selected-color value)
                         (let [first-size (->> (:color @unit)
                                               (filter #(= (:color %) value))
                                               (mapcat :size)
                                               (map :size)
                                               (remove nil?)
                                               first)
                               product_id (when (and value first-size)
                                            (->> (:color @unit)
                                                 (filter #(= (:color %) value))
                                                 first
                                                 :size
                                                 (filter #(= (:size %) first-size))
                                                 first
                                                 :product_id))]
                           (reset! selected-size first-size)
                           (when product_id
                             (set! (.-href (.-location js/window))
                                   (str "#/product/" (@unit :unit_id) "/" product_id))
                             )
                           )
                         ) 
            set-size! (fn [value]
                        (reset! selected-size value)
                        (let [product_id (when (and @selected-color value)
                                           (->> (:color @unit)
                                                (filter #(= (:color %) @selected-color))
                                                first
                                                :size
                                                (filter #(= (:size %) value))
                                                first
                                                :product_id))]
                          (when product_id
                            (swap! app-state assoc :current_product_id product_id)))
                        (when (not (some #(= % value) filtered-sizes))
                          (reset! selected-color nil)))

            selected-color-product-id (:product_id @product_current) 

            parse-int (fn [value]
                        (cond
                          (number? value) value
                          (string? value) (let [parsed (js/parseInt value 10)]
                                            (when-not (js/isNaN parsed) parsed))
                          :else nil))
            cart-item (when selected-color-product-id
                        (->> @cart
                             (keep (fn [ci]
                                     (when (= (:product_id ci) selected-color-product-id)
                                       (let [qty (parse-int (:quantity ci))]
                                         (when (and (number? qty) (> qty 0))
                                           (assoc ci :quantity qty))))))
                             first))

            raw-stock (or (:stock_quantity cart-item)
                          (:stock_quantity @product_current)
                          (:quantity @product_current))
            stock-quantity (cond
                             (number? raw-stock) raw-stock
                             (string? raw-stock) (let [parsed (js/parseInt raw-stock 10)]
                                                   (when-not (js/isNaN parsed) parsed))
                             :else nil)
            cart-quantity (when cart-item (:quantity cart-item))
            overstocked? (and (number? stock-quantity)
                              cart-quantity
                              (> cart-quantity stock-quantity))
            actual-raw (:actual @product_current)
            actual? (cond
                      (nil? actual-raw) true
                      (string? actual-raw) (= "true" (str/lower-case actual-raw))
                      (boolean? actual-raw) actual-raw
                      :else true)
            in-stock? (and actual?
                           (if (number? stock-quantity)
                             (> stock-quantity 0)
                             true))
            prices (:prices @product_current)] 

        (if (= @product_current {})
          [:> Skeleton {:active true}]


          [:> Space {:direction "vertical"
                     :style {:width "100%"
                             :padding "24px 20px"
                             :text-align "left"
                             :background "linear-gradient(135deg, #fafafa 0%, #ffffff 100%)"
                             :minHeight "100vh"}}

           (let [telegram_price (:price (:telegram_price prices))
                 discount_price (:price (:discount_price prices))
                 share-url (str "https://t.me/share/url?url="
                                (let [unit-id (:unit_id @unit)
                                      product-id (:product_id @product_current)
                                      data-json (js/JSON.stringify
                                                 (clj->js {:type "product"
                                                           :u unit-id
                                                           :p product-id}))
                                      encoded-data (.replace
                                                    (.replace
                                                     (js/btoa data-json)
                                                     #"\+" "-")
                                                    #"/" "_")
                                      share-url (str "https://t.me/" (:bot_name (:config @app-state)) "?startapp=" encoded-data)]
                                  (js/encodeURIComponent share-url))
                                "&text=" (js/encodeURIComponent (:product_name @unit)))]
             [:div {:style {:display "flex"
                            :justify-content "space-between"
                            :align-items "center"
                            :margin-bottom 8}}
              [:div {:style {:display "flex"
                             :gap 10
                             :align-items "center"}}
               (if discount_price
                 [:<>
                  [:span {:style {:font-family "Roboto Flex"
                                  :font-size "40px"
                                  :font-weight 400
                                  :line-height "100%"
                                  :color "var(--color-accent)"}}
                   (str discount_price "₽")]
                  [:span {:className "price-strike"
                          :style {:font-family "Roboto Flex"
                                  :font-size "22px"
                                  :font-weight 100
                                  :line-height "100%"
                                  :color "#8c8c8c"}}
                   (str telegram_price "₽")]]
                 (when telegram_price
                   [:span {:style {:font-family "Roboto Flex"
                                   :font-size "40px"
                                   :font-weight 500
                                   :line-height "100%"
                                   :color "var(--color-accent)"}}
                    (str telegram_price "₽")]))]

              [:> antd/Typography.Link
               {:href share-url
                :onClick (fn []
                           (.impactOccurred (.-HapticFeedback (.-WebApp js/Telegram)) "light"))
                :style {:display "flex"
                        :alignItems "center"
                        :justifyContent "center"
                        :width 36
                        :height 36
                        :border "1px solid #cfcfcf"
                        :borderRadius "50%"
                        :background "transparent"}}
               
               [:img {:src "share.svg"
                      :alt "Поделиться"
                      :style {:width 18
                              :height 18
                              :display "block"}}
                ]
               
               ]
              ]
              )

           [:div {:style {:font-size 32
                          :font-weight 400
                          :line-height "120%"
                          :margin-bottom 8
                          :color "#111"}}
            (:product_name @unit)]

           [:> antd/Typography.Text {:style {:color "#8c8c8c"
                                        :font-family "Roboto Flex"
                                        :fontSize 12
                                        :letterSpacing "0.5px"
                                        :marginBottom 20
                                        :display "block"}}
            (str "Артикул " selected-color-product-id)]
           
           
           ;; Выбор цвета
           [:div {:style {:marginBottom 24}}
            [:div {:style {:fontSize 30
                           :font-family "Roboto Flex"
                           :fontWeight 500
                           :color "#1f1f1f"
                           :marginBottom 12}}
             "Цвет"]
            [:div {:style {:display "flex"
                           :gap 12
                           :flexWrap "wrap"}}
             (for [color all-colors]
               ^{:key color}
               [:button {:type "button"
                         :onClick (fn [] (set-color! color))
                         :style {:height 36
                                 :padding "0 16px"
                                 :borderRadius "999px"
                                 :font-family "Roboto Flex"
                                 :fontWeight 100
                                 :border (str "1px solid "
                                              (if (= color @selected-color)
                                                accent-color
                                                border-color))
                                 :background (if (= color @selected-color)
                                               "#0b0b0b"
                                               "transparent")
                                 :color (if (= color @selected-color)
                                          "#ffffff"
                                          "#1f1f1f")
                                 :fontSize 14
                                 :lineHeight "100%"
                                 :cursor "pointer"}}
                color])]]

           ;; Выбор размера
           [:div {:style {:marginBottom 24}}
            [:div {:style {:fontSize 30
                           :font-family "Roboto Flex"
                           :fontWeight 500
                           :color "#1f1f1f"
                           :marginBottom 12}}
             "Размер"]
            [:div {:style {:display "flex"
                           :gap 12
                           :flexWrap "wrap"}}
             (for [size filtered-sizes]
               (let [size-label (str size)
                     long-size? (> (count size-label) 2)
                     selected? (= size @selected-size)]
                 ^{:key size}
                 [:button {:type "button"
                           :onClick (fn [] (set-size! size))
                           :style (merge
                                   {:height 36
                                    :font-family "Roboto Flex"
                                    :fontWeight 100
                                    :border (str "1px solid "
                                                 (if selected?
                                                   accent-color
                                                   border-color))
                                    :background (if selected?
                                                  "#0b0b0b"
                                                  "transparent")
                                    :color (if selected?
                                             "#ffffff"
                                             "#1f1f1f")
                                    :fontSize 12
                                    :lineHeight "100%"
                                    :cursor "pointer"
                                    :padding 0}
                                   (if long-size?
                                     {:minWidth 64
                                      :padding "0 16px"
                                      :borderRadius "999px"}
                                     {:width 36
                                      :borderRadius "50%"}))}
                  size-label]))]]

           (when (and overstocked? (pos? stock-quantity))
             [:div {:style {:border (str "1px solid " accent-color)
                            :borderRadius 14
                            :padding "10px 12px"
                            :marginBottom 16
                            :display "flex"
                            :alignItems "center"
                            :justifyContent "space-between"
                            :gap 12
                            :color accent-color}}
              [:div {:style {:fontSize 14
                             :lineHeight "18px"}}
               (str "В наличии осталось " stock-quantity " шт.")]
              [:button {:type "button"
                        :onClick (fn []
                                   (cart_set selected-color-product-id stock-quantity))
                        :style {:border "none"
                                :background accent-color
                                :color "#ffffff"
                                :borderRadius 12
                                :padding "6px 10px"
                                :fontSize 12
                                :fontWeight 500
                                :cursor "pointer"}}
               (str "Оставить " stock-quantity)]])

           (cond
             (not in-stock?)
             [:> Button
              {:style {:borderRadius 31
                       :height 62
                       :width "100%"
                       :background-color "#e5e5e5"
                       :font-family "Roboto Flex"
                       :fontWeight 500
                       :font-size 24
                       :lineHeight "100%"
                       :letterSpacing "0"
                       :color "#8c8c8c"
                       :border "none"
                       :padding 0
                       :display "flex"
                       :alignItems "center"
                       :justifyContent "center"}
               :type "default"
               :disabled true}
              "нет в наличии"]

             cart-item
             [:div {:style {:display "flex"
                            :align-items "center"
                            :justify-content "space-between"
                            :height 62
                            :width "100%"
                            :borderRadius 31
                            :background "#ffffff"
                            :border (str "2px solid " accent-color)
                            :padding "0 20px"
                            :color accent-color}}
              [:button {:type "button"
                        :onClick (fn []
                                   (cart_set selected-color-product-id (dec (:quantity cart-item))))
                        :style {:background "transparent"
                                :border "none"
                                :color accent-color
                                :width 34
                                :height 34
                                :display "flex"
                                :alignItems "center"
                                :justifyContent "center"
                                :cursor "pointer"}}
               [:> MinusOutlined {:style {:fontSize 22}}]]

              [:div {:style {:fontSize 24
                             :fontWeight 500
                             :color accent-color}}
               (:quantity cart-item)
               ]

              [:button {:type "button"
                        :onClick (fn []
                                   (let [next-qty (inc (:quantity cart-item))]
                                     (when (or (nil? stock-quantity)
                                               (<= next-qty stock-quantity))
                                       (cart_set selected-color-product-id next-qty))))
                        :style {:background "transparent"
                                :border "none"
                                :color accent-color
                                :width 34
                                :height 34
                                :display "flex"
                                :alignItems "center"
                                :justifyContent "center"
                                :cursor "pointer"}}
               [:> PlusOutlined {:style {:fontSize 22}} ]
               ]
              ]

             :else
             [:> Button
              {:style {:borderRadius 31
                       :height 62
                       :width "100%"
                       :background-color "var(--color-accent)"
                       :font-family "Roboto Flex"
                       :fontWeight 400
                       :font-size 24
                       :lineHeight "100%"
                       :letterSpacing "0"
                       :color "#ffffff"
                       :border "none"
                       :padding 0
                       :display "flex"
                       :alignItems "center"
                       :justifyContent "center"}
               :type "primary"
               :onClick (fn []
                          (cart_set selected-color-product-id 1))}
              "Добавить в корзину"]
             )

           ;; Описание товара
           (if-not (= "" (str (:product_description @product_current)))
             [:div {:style {:marginTop 24
                            :marginBottom 24}}
              [:div {:style {:display "flex"
                             :alignItems "center"
                             :gap 12
                             :cursor "pointer"
                             :userSelect "none"}
                     :onClick (fn []
                                (swap! desc-open? not))}
               [:div {:style {:fontSize 24
                              :fontWeight 500
                              :color "#111"}}
                "Описание"]
               [:div {:style {:position "relative"
                              :width 20
                              :height 14}}
                [:svg {:width 20
                       :height 14
                       :viewBox "0 0 24 16"
                       :style {:position "absolute"
                               :top 0
                               :left 0
                               :opacity (if @desc-open? 1 0)
                               :transform (if @desc-open?
                                            "translateY(0)"
                                            "translateY(4px)")
                               :transition "opacity 0.2s ease, transform 0.2s ease"}}
                 [:polygon {:points "12,2 22,14 2,14"
                            :fill "none"
                            :stroke accent-color
                            :strokeWidth 2}]]
                [:svg {:width 20
                       :height 14
                       :viewBox "0 0 24 16"
                       :style {:position "absolute"
                               :top 0
                               :left 0
                               :opacity (if @desc-open? 0 1)
                               :transform (if @desc-open?
                                            "translateY(-4px)"
                                            "translateY(0)")
                               :transition "opacity 0.2s ease, transform 0.2s ease"}}
                 [:polygon {:points "2,2 22,2 12,14"
                            :fill accent-color}]]]]
              [:div {:style {:maxHeight (if @desc-open? 400 0)
                             :opacity (if @desc-open? 1 0)
                             :transform (if @desc-open?
                                          "translateY(0)"
                                          "translateY(-4px)")
                             :transition "max-height 0.25s ease, opacity 0.2s ease, transform 0.2s ease"
                             :overflow "hidden"}}
               [:div {:style {:whiteSpace "pre-line"
                              :color "#6f6f6f"
                              :lineHeight 1.4
                              :marginTop 12}}
                (:product_description @product_current)
                ]
               ]
              ]
              )
           

           ;; Дополнительная информация
           (if-not (= "" (str (:made_of @product_current)))
             [:div {:style {:marginBottom 24}}
              [:div {:style {:display "flex"
                             :alignItems "center"
                             :gap 12
                             :cursor "pointer"
                             :userSelect "none"}
                     :onClick (fn []
                                (swap! fabric-open? not))}
               [:div {:style {:fontSize 24
                              :fontWeight 500
                              :color "#111"}}
                "Состав"]
               [:div {:style {:position "relative"
                              :width 20
                              :height 14}}
                [:svg {:width 20
                       :height 14
                       :viewBox "0 0 24 16"
                       :style {:position "absolute"
                               :top 0
                               :left 0
                               :opacity (if @fabric-open? 1 0)
                               :transform (if @fabric-open?
                                            "translateY(0)"
                                            "translateY(4px)")
                               :transition "opacity 0.2s ease, transform 0.2s ease"}}
                 [:polygon {:points "12,2 22,14 2,14"
                            :fill "none"
                            :stroke accent-color
                            :strokeWidth 2}]]
                [:svg {:width 20
                       :height 14
                       :viewBox "0 0 24 16"
                       :style {:position "absolute"
                               :top 0
                               :left 0
                               :opacity (if @fabric-open? 0 1)
                               :transform (if @fabric-open?
                                            "translateY(-4px)"
                                            "translateY(0)")
                               :transition "opacity 0.2s ease, transform 0.2s ease"}}
                 [:polygon {:points "2,2 22,2 12,14"
                            :fill accent-color}]]]]
              [:div {:style {:maxHeight (if @fabric-open? 240 0)
                             :opacity (if @fabric-open? 1 0)
                             :paddingTop (if @fabric-open? 12 0)
                             :boxSizing "border-box"
                             :transition "max-height 0.35s ease, opacity 0.25s ease, padding-top 0.35s ease"
                             :overflow "hidden"
                             :willChange "max-height, opacity, padding-top"}}
               [:div {:style {:whiteSpace "pre-line"
                              :color "#6f6f6f"
                              :lineHeight 1.4}}
                (:made_of @product_current)]
               ]
              ]
              )

           (if-not (= "" (str (:product_care_info @product_current)))
             [:div {:style {:marginBottom 24}}
              [:div {:style {:display "flex"
                             :alignItems "center"
                             :gap 12
                             :cursor "pointer"
                             :userSelect "none"}
                     :onClick (fn []
                                (swap! sizes-open? not))}
               [:div {:style {:fontSize 24
                              :fontWeight 500
                              :color "#111"}}
                "Уход"]
               [:div {:style {:position "relative"
                              :width 20
                              :height 14}}
                [:svg {:width 20
                       :height 14
                       :viewBox "0 0 24 16"
                       :style {:position "absolute"
                               :top 0
                               :left 0
                               :opacity (if @sizes-open? 1 0)
                               :transform (if @sizes-open?
                                            "translateY(0)"
                                            "translateY(4px)")
                               :transition "opacity 0.2s ease, transform 0.2s ease"}}
                 [:polygon {:points "12,2 22,14 2,14"
                            :fill "none"
                            :stroke accent-color
                            :strokeWidth 2}]]
                [:svg {:width 20
                       :height 14
                       :viewBox "0 0 24 16"
                       :style {:position "absolute"
                               :top 0
                               :left 0
                               :opacity (if @sizes-open? 0 1)
                               :transform (if @sizes-open?
                                            "translateY(-4px)"
                                            "translateY(0)")
                               :transition "opacity 0.2s ease, transform 0.2s ease"}}
                 [:polygon {:points "2,2 22,2 12,14"
                            :fill accent-color}]]]]
              [:div {:style {:maxHeight (if @sizes-open? 240 0)
                             :opacity (if @sizes-open? 1 0)
                             :paddingTop (if @sizes-open? 12 0)
                             :boxSizing "border-box"
                             :transition "max-height 0.35s ease, opacity 0.25s ease, padding-top 0.35s ease"
                             :overflow "hidden"
                             :willChange "max-height, opacity, padding-top"}}
               [:div {:style {:whiteSpace "pre-line"
                              :color "#6f6f6f"
                              :lineHeight 1.4}}
                (:product_care_info @product_current)]
               ]
              ]
              )

           ;; Отступ внизу для мобильных устройств
           [:div {:style {:height 20}}]])))))
