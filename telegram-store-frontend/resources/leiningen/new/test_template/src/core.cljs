(ns {{name}}.core
  (:require
   ["antd" :as antd]
   [reagent.core :as reagent]
   [reagent.dom :as d]

   [{{name}}.db :refer [app-state]]
   [{{name}}.router :refer [routes]]
   [{{name}}.viewes :refer [current-page]]
   [{{name}}.pages.policies.policies :refer [policies]]

   [clojure.string :as str]
   [clojure.edn :as edn]



   [{{name}}.events.policies-get        :refer [policies_get]]
   [{{name}}.events.cart-get            :refer [cart_get]]
   [{{name}}.events.cart-get-summary    :refer [cart_get_summary]] 
   [{{name}}.events.cart-to-checkout    :refer [cart_to_checkout]]
   [{{name}}.events.filters-get         :refer [filters_get]]
   [{{name}}.events.catalog-config-get  :refer [catalog_config_get]] 
   [{{name}}.events.products-get        :refer [products_get]] 
   [{{name}}.events.unit-get            :refer [unit_get]]
   [{{name}}.events.product-get-single  :refer [product_get]]
   [{{name}}.events.banners-get         :refer [banner_get]]
   [{{name}}.events.user-get-init       :refer [user_get_init]]
   [{{name}}.events.payment-add         :refer [payment_add]]
   [{{name}}.events.settings-get        :refer [settings_get]]
   [{{name}}.events.policies-get-all    :refer [policies_get_all]]
   [{{name}}.events.user-add            :refer [user_add]]
   [{{name}}.events.favorite-get        :refer [favorite_get]]
   [{{name}}.events.store-get           :refer [store_get]]
   )
  )



(defn is-mobile? []
  (let [user-agent (.-userAgent (.-navigator js/window))
        mobile-regex #"(?i)android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini"]
    (boolean (re-find mobile-regex user-agent))))

(defn input-like? [el]
  (when el
    (let [tag (.-tagName el)]
      (or (= tag "INPUT")
          (= tag "TEXTAREA")
          (= tag "SELECT")
          (.-isContentEditable el)))))

(defn closest? [el selector]
  (when (and el (.-closest el))
    (.closest el selector)))

(defn within-drawer? [el]
  (closest? el ".cdek-drawer"))

(def allowed-input-target-selector
  "input, textarea, select, [contenteditable='true'], .ant-input, .ant-input-affix-wrapper, .ant-input-number, .ant-select, .ant-picker")

(defonce suppress-scroll-blur-until (reagent/atom 0))
(def suppress-scroll-blur-ms 700)

(defn now-ms []
  (js/Date.now))

(defn blur-active-input! []
  (when-let [active (.-activeElement js/document)]
    (when (input-like? active)
      (.blur active))))

(defn handle-scroll-blur! [_]
  (when (and (>= (now-ms) @suppress-scroll-blur-until)
             (not (within-drawer? (.-activeElement js/document))))
    (blur-active-input!)))

(defn handle-focusin! [e]
  (let [target (.-target e)]
    (when (input-like? target)
      (reset! suppress-scroll-blur-until (+ (now-ms) suppress-scroll-blur-ms)))))

(defn handle-tap-blur! [e]
  (let [target (.-target e)
        active (.-activeElement js/document)
        within-allowed? (when (and target (.-closest target))
                          (.closest target allowed-input-target-selector))
        within-drawer-target? (within-drawer? target)]
    (when (and (input-like? active)
               (not within-allowed?)
               (not within-drawer-target?))
      (.blur active))))

(defonce keyboard-dismiss-installed? (reagent/atom false))
(def keyboard-dismiss-enabled? false)

(defn hook-keyboard-dismiss! []
  (when (and keyboard-dismiss-enabled?
             (not @keyboard-dismiss-installed?))
    (reset! keyboard-dismiss-installed? true)
    (.addEventListener js/window "scroll" handle-scroll-blur! #js {:passive true})
    (.addEventListener js/window "touchmove" handle-scroll-blur! #js {:passive true})
    (.addEventListener js/document "focusin" handle-focusin! #js {:capture true})
    (.addEventListener js/document "touchstart" handle-tap-blur! #js {:passive true :capture true})
    (.addEventListener js/document "mousedown" handle-tap-blur! #js {:passive true :capture true})))

(defn parse-date [s]
  (when (and s (not (str/blank? s)))
    (let [d (js/Date. s)]
      (when-not (js/isNaN (.getTime d)) d))))

(defn store-tariff-active? [store]
  (let [current-tariff (:current_tariff store)
        ended-at       (:ended_at current-tariff)
        planned-until  (:planned_until current-tariff)
        planned-date   (parse-date planned-until)]
    (cond
      (nil? current-tariff) false
      (some? ended-at)      false
      planned-date          (>= (.getTime planned-date) (.getTime (js/Date.)))
      :else                 true)))

(defn valid-email? [value]
  (let [mail (or value "")]
    (boolean (re-matches #"^[^\s@]+@[^\s@]+(\.[^\s@]+)*$" mail))))

(defn delivery-errors [shipping-data]
  (let [surname (or (:surname shipping-data) "")
        first-name (or (:first_name shipping-data) "")
        mail (or (:mail shipping-data) "")
        email-valid? (and (not (str/blank? mail))
                          (valid-email? mail))
        phone (or (:phone shipping-data) "")]
    (cond-> {}
      (str/blank? surname) (assoc :surname "Введите фамилию")
      (str/blank? first-name) (assoc :first_name "Введите имя")
      (str/blank? mail) (assoc :mail "Введите email")
      (and (not (str/blank? mail)) (not email-valid?))
      (assoc :mail "Введите корректный email")
      (or (str/blank? phone) (not (re-matches #"^\+7\d{10}$" phone)))
      (assoc :phone "Введите телефон полностью"))))

(def delivery-field-ids
  {:surname "delivery-surname"
   :first_name "delivery-first-name"
   :mail "delivery-mail"
   :phone "delivery-phone"})

(defn empty-state []
  [:div {:style {:padding "18px 0"
                 :display "flex"
                 :flexDirection "column"
                 :alignItems "center"
                 :justifyContent "center"
                 :color "#8c8c8c"
                 :gap 8}}
   [:div {:style {:width 64
                  :height 48
                  :border "1px dashed #d9d9d9"
                  :borderRadius 8
                  :position "relative"}}
    [:div {:style {:position "absolute"
                   :top 10
                   :left 10
                   :right 10
                   :height 6
                   :background "#ededed"
                   :borderRadius 4}}]
    ]
   [:div {:style {:fontSize 13}}
    "Пока ничего нет"]])

(defonce context-menu-disabled? (atom false))

(defn disable-image-context-menu! []
  (when-not @context-menu-disabled?
    (reset! context-menu-disabled? true)
    (.addEventListener js/document "contextmenu"
                       (fn [event]
                         (let [target (.-target event)]
                           (when (and target (= "IMG" (.-tagName target)))
                             (.preventDefault event)))))))


(defn page_template []
  (let [Space antd/Space
        Button antd/Button
        Layout antd/Layout
        Content (.-Content Layout)
        ConfigProvider antd/ConfigProvider

        web-app (.-WebApp js/Telegram)

        platform (when web-app (.-platform web-app))
        is-tg-mobile? (contains? #{"ios" "android"} platform)
        is-mobile-device? (is-mobile?)
        is-mobile (or is-tg-mobile? is-mobile-device?)
        ]
    (fn []
      (let [app-ready? (:app_ready? @app-state)
            technical-work? (:technical_work? @app-state)
            user-status (:user_status @app-state)
            admin-work-banner? (and technical-work? (= user-status "admin"))
            store (:store @app-state)
            tariff-active? (or (nil? store) (store-tariff-active? store))
            store-inactive? (and (some? store) (not tariff-active?))
            privileged-user? (contains? #{"admin" "owner" "manager"} user-status)
            ]
        [:> ConfigProvider {:theme {:components {}}
                            :renderEmpty (fn []
                                           (reagent/as-element [empty-state]))
                            :wave {:disabled true}}
        [:> Layout {:style {:overflow-y "hidden"
                            :background "#ffffff"}}
          [:> Content
           {:style {:display "flex"
                    :flex-direction "column"
                    :min-height "100vh"
                    :background "#ffffff"}
            }
           [:style "img{-webkit-touch-callout:none;-webkit-user-select:none;user-select:none;-webkit-user-drag:none;user-drag:none;pointer-events:none;}"]
           (when admin-work-banner?
             [:div {:style {:position "sticky"
                            :top 0
                            :zIndex 20
                            :background "#fff3f3"
                            :borderBottom "1px solid #f5c2c2"
                            :color "#111"
                            :padding "8px 16px"
                            :fontSize 12
                            :textAlign "center"}}
              "Технические работы"]
             )
           (when (and store-inactive? privileged-user?)
             [:div {:style {:position "sticky"
                            :top 0
                            :zIndex 20
                            :background "#fff3cd"
                            :borderBottom "1px solid #ffc107"
                            :color "#111"
                            :padding "8px 16px"
                            :fontSize 12
                            :textAlign "center"
                            :cursor "pointer"}
                    :onClick (fn [] (.openTelegramLink web-app "https://t.me/bibi_zen_bot"))}
              "МАГАЗИН НЕАКТИВЕН. Требуется оплата доступа"])
           [policies]

           (cond
             (and store-inactive? (not privileged-user?))
             [:div {:style {:padding 26
                            :height "100vh"
                            :box-sizing "border-box"}}
              [:> Space {:direction "vertical"
                         :style {:border-radius 10
                                 :width "100%"
                                 :height "100%"
                                 :padding 18
                                 :border "1px solid #d9d9d9"
                                 :display "flex"
                                 :justify-content "center"
                                 :text-align "center"}}
               [:div {:style {:white-space "pre-wrap"
                              :font-size 18}}
                "Магазин временно неактивен."]]]

             (or (not technical-work?) (and technical-work? (= user-status "admin")))
             [current-page]

             :else
             [:div {:style {:padding 26
                            :height "100vh"
                            :box-sizing "border-box"}}
              [:> Space {:direction "vertical"
                         :style {:border-radius 10
                                 :width "100%"
                                 :height "100%"
                                 :padding 18
                                 :border "1px solid black"
                                 :display "flex"
                                 :justify-content "center"
                                 :text-align "center"}}
               [:div {:style {:white-space "pre-wrap"
                              :font-size 18}}
                "Ведутся технические работы. Скоро все заработает."]]]
             )

           [:div {:style {:text-align "center"
                          :font-size "12px"
                          :margin-top "auto"
                          :padding-top 10
                          :padding-bottom 10
                          :background "#f5f5f5"
                          :color "#777"}
                  :onClick (fn []
                             (.openTelegramLink web-app "https://t.me/bibi_zen_bot"))}
            "разработал BIBI-ZEN ©"]

           ]
           ]
           ]
           
           ))))


(defn mount-root []
  (let [main_button js/Telegram.WebApp.MainButton
        back-button js/Telegram.WebApp.BackButton

        texting? (reagent/cursor app-state [:texting?])]

    (hook-keyboard-dismiss!)
    (disable-image-context-menu!)

    (add-watch app-state :page_listener (fn [key atom old-state new-state] 
                                          
                                          (if (and (= (old-state :page) :product) (not (= (new-state :page) :product)))
                                            (do
                                              (swap! app-state assoc :product_current {}) 
                                              )
                                            )
                                        
                                          (if-not (= (:current_product_id new-state) (:current_product_id old-state))
                                            (do
                                              (if-not (nil? (:current_product_id new-state))
                                                (product_get (:current_product_id new-state))
                                                )
                                              )
                                            )
                                        
                                          (if-not (= (:search_value new-state) (:search_value old-state))
                                            (products_get (:search_value new-state) (:filters_picked new-state) (:selected_sorting new-state)))
                                          
                                          (if-not (= (:selected_sorting new-state) (:selected_sorting old-state))
                                            (products_get (:search_value new-state) (:filters_picked new-state) (:selected_sorting new-state)))
                                        
                                          (if-not (= (:current_unit_id new-state) (:current_unit_id old-state))
                                            (do
                                              (if-not (nil? (:current_unit_id new-state))
                                                (unit_get (:current_unit_id new-state))
                                                ) 
                                              )
                                            )
                                        
                                        
                                          (if-not (= (:filters_picked new-state) (:filters_picked old-state))
                                            (products_get (:search_value new-state) (:filters_picked new-state) (:selected_sorting new-state)))
                                        
                                        
                                          (if-not (= (:page new-state) (:page old-state))
                                            (if-not (or (= :payment_description (:page old-state)) (= :product (:page old-state)) (= :delivery (:page old-state)))
                                              (swap! app-state assoc :prev_page (:page old-state))))
                                        
                                          (if (= :catalog (:page new-state))
                                            (.hide back-button)
                                            (.show back-button))
                                        
                                          (let [drawer-open? (or (:policies_menu_open? new-state)
                                                                 (:sort_menu_open new-state)
                                                                 (:filters_menu_open new-state)
                                                                 (:side_menu_open new-state)
                                                                 (not (:app_ready? new-state)))
                                                page (:page new-state)
                                                hide-main-button? (= :thank-you page)
                                                hide-for-technical-work? (and (= "normis" (:user_status new-state))
                                                                              (:technical_work? new-state))
                                                payment-methods (:payment_methods new-state)
                                                methods (cond
                                                          (vector? payment-methods) payment-methods
                                                          (sequential? payment-methods) (vec payment-methods)
                                                          (map? payment-methods) [payment-methods]
                                                          :else [])
                                                has-payment-method? (boolean
                                                                     (some (fn [{:keys [payment_provider_name is_active]}]
                                                                             (and payment_provider_name is_active))
                                                                           methods))
                                                block-main? (and (= :delivery page)
                                                                 (not has-payment-method?)
                                                                 )
                                                ]
                                            (if (is-mobile?)
                                              (if (and (not hide-main-button?)
                                                       (not hide-for-technical-work?)
                                                       (not block-main?)
                                                       (not drawer-open?)
                                                       (or (= :catalog page)
                                                           (not @texting?))
                                                       (not (empty? (:cart new-state))))
                                                (.show main_button)
                                                (.hide main_button))
                                              (if (and (not hide-main-button?)
                                                       (not hide-for-technical-work?)
                                                       (not block-main?)
                                                       (not drawer-open?)
                                                       (not (empty? (:cart new-state))))
                                                (.show main_button)
                                                (.hide main_button))
                                              )
                                            )
                                        
                                          (cond
                                            (= :catalog  (:page new-state)) (.setText main_button "Корзина")
                                        
                                            (= :product  (:page new-state)) (.setText main_button "Корзина")
                                        
                                            (= :cart     (:page new-state)) (.setText main_button "Оформить заказ")
                                        
                                            (= :delivery (:page new-state)) (.setText main_button "Оплатить")

                                            (= :thank-you (:page new-state)) (.setText main_button "В каталог")))
                                          )
                                          )

  (routes)
  (d/render [page_template] (.getElementById js/document "app")))


(def message (.-message antd))


(defn load-config []
  (-> (js/fetch "config.edn")
      (.then (fn [response] (.text response)))
      (.then (fn [text] (edn/read-string text)))
      (.catch (fn [error] (js/console.error "Error loading config:" error)))
      (.then (fn [config]
               (swap! app-state assoc :config config)
               (user_add (:raw_start_param @app-state))
               (policies_get_all)
               (user_get_init)
               (store_get)
               (settings_get)
               (filters_get) 
               (catalog_config_get)
               (banner_get {:banner_location "main_page"})
               (products_get (:search_value @app-state) (:filters_picked @app-state) (:selected_sorting @app-state))
               (cart_get)
               (cart_get_summary) 
               (favorite_get)
               (mount-root) 
               )
             )
      )
  )




(defn scroll-to-top []
  (js/window.scrollTo 0 0)
  )

(defn ^:export init! []
  (let [
        web-app (.-WebApp js/Telegram)
        user (.. js/Telegram -WebApp -initDataUnsafe -user)
        start_param (.. js/Telegram -WebApp -initDataUnsafe -start_param)
        back-button (.-BackButton web-app)
        main_button js/Telegram.WebApp.MainButton
        ] 
    
    (when (is-mobile?) 
      (.requestFullscreen web-app)
      (.disableVerticalSwipes web-app)
      (.lockOrientation web-app)
      )
    
    (.setParams main_button #js {:color "#000000"
                                 :textColor "#FFFFFF"}
                )
    
    (if-not (nil? start_param)
      (try
        (let [decoded-data (js/atob (.replace
                                     (.replace start_param #"-" "+")
                                     #"_" "/"))

              data (js->clj (js/JSON.parse decoded-data) :keywordize-keys true)
              share-type (:type data)
              ref-code (or (:ref data) (:promo data) (:startbot data))]

          (when (and ref-code (not (str/blank? ref-code)))
            (swap! app-state assoc :raw_start_param ref-code))

          (case share-type
            "product" (let [unit-id (:u data)
                            product-id (:p data)]
                        (set! (.-href (.-location js/window)) (str "#/product/" unit-id "/" product-id))
                        )
            "order" (let [order-id (or (:order_id data) (:o data))]
                      (when order-id
                        (set! (.-href (.-location js/window)) (str "#/order/" order-id))))

            (let [filters (:f data)
                  search-string (:s data)]
              (when filters
                (swap! app-state assoc :filters_picked filters))
              (when search-string
                (swap! app-state assoc :search_string search-string)))))

        (catch js/Error e
          (js/console.error "Error parsing start parameter:" e)
          (swap! app-state assoc :raw_start_param start_param)
          )
        )
        )
    
    (load-config)

    (.setBottomBarColor web-app "#FFFFFF")
    (.setText main_button "Корзина")


    (.onClick main_button (fn []
                            (.impactOccurred (.-HapticFeedback (.-WebApp js/Telegram)) "medium")

                            (case (:page @app-state)
                              :cart (do 
                                      ;переход на страницу оформления заказа. сначала - проверяем, актуально ли количество товара
                                      ;в корзине с тем, сколько на складе
                                      (.showProgress main_button) 
                                      (cart_to_checkout)
                                      )
                              
                              :delivery (let [errors (delivery-errors (:shipping_data @app-state))]
                                          (if (seq errors)
                                            (do
                                              (swap! app-state assoc :delivery_errors errors)
                                              (js/setTimeout
                                               (fn []
                                                 (let [ordered-keys [:surname :first_name :mail :phone]
                                                       first-error (first (filter #(contains? errors %) ordered-keys))
                                                       target-id (get delivery-field-ids first-error)]
                                                   (when-let [el (.getElementById js/document target-id)]
                                                     (.scrollIntoView el #js {:behavior "smooth"
                                                                              :block "center"}))))
                                               0))
                                            (do
                                              (.showProgress main_button) 
                                              (swap! app-state assoc :delivery_errors {})
                                              (payment_add)
                                              )
                                            )
                                          )
                             

                              :information (do
                                             (swap! app-state assoc :page :cart)
                                             (set! (.-href (.-location js/window)) "#/cart")
                                             (scroll-to-top)
                                             ) 

                              :product (do
                                         (swap! app-state assoc :page :cart)
                                         (set! (.-href (.-location js/window)) "#/cart")
                                         (scroll-to-top)
                                         )

                              :catalog (do
                                         (swap! app-state assoc :page :cart)
                                         (set! (.-href (.-location js/window)) "#/cart")
                                         (scroll-to-top)
                                         )
                              
                              :favorites (do
                                          (swap! app-state assoc :page :cart)
                                          (set! (.-href (.-location js/window)) "#/cart")
                                          (scroll-to-top)
                                          )
                              
                              :orders-history (do
                                                (swap! app-state assoc :page :cart)
                                                (set! (.-href (.-location js/window)) "#/cart")
                                                (scroll-to-top)
                                                )
                              
                              :order (do
                                       (swap! app-state assoc :page :cart)
                                       (set! (.-href (.-location js/window)) "#/cart")
                                       (scroll-to-top)
                                       )

                              :thank-you (do
                                           (swap! app-state assoc :page :catalog)
                                           (set! (.-href (.-location js/window)) "#/catalog")
                                           (scroll-to-top)
                                           )
                              )
                            )
              )


    (.onClick back-button (fn []
                            (case (:page @app-state)
                              :product (do
                                         (case (:prev_page @app-state)
                                           :catalog (do
                                                      (swap! app-state assoc :page :catalog)
                                                      (set! (.-href (.-location js/window)) "#/catalog") 
                                                      )
                                           :cart (do
                                                   (swap! app-state assoc :page :cart)
                                                   (set! (.-href (.-location js/window)) "#/cart")
                                                   )
                                           :favorites (do
                                                        (swap! app-state assoc :page :favorites)
                                                        (set! (.-href (.-location js/window)) "#/favorites")
                                                        )
                                           (do
                                             (swap! app-state assoc :page :catalog)
                                             (set! (.-href (.-location js/window)) "#/catalog"))
                                           )
                                         (swap! app-state assoc :prev_page nil)
                                         )

                              :cart (do
                                      (swap! app-state assoc :page :catalog)
                                      (set! (.-href (.-location js/window)) "#/catalog"))

                              :delivery (do
                                          (swap! app-state assoc :page :cart)
                                          (set! (.-href (.-location js/window)) "#/cart")
                                          )

                              :information (do
                                             (swap! app-state assoc :page :catalog)
                                             (set! (.-href (.-location js/window)) "#/catalog")
                                             )
                              
                              :orders-history (do
                                                (swap! app-state assoc :page :catalog)
                                                (set! (.-href (.-location js/window)) "#/catalog")
                                                )
                              
                              :favorites (do
                                           (swap! app-state assoc :page :catalog)
                                           (set! (.-href (.-location js/window)) "#/catalog")
                                           )
                              
                              :order (do
                                       (swap! app-state assoc :page :orders-history)
                                       (set! (.-href (.-location js/window)) "#/orders-history")
                                       )

                              :thank-you (do
                                           (swap! app-state assoc :page :catalog)
                                           (set! (.-href (.-location js/window)) "#/catalog")
                                           )
                              
                              )
                            )
              ) 
    )
    )
