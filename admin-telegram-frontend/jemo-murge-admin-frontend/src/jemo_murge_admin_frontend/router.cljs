(ns jemo-murge-admin-frontend.router
  (:import goog.history.Html5History
           goog.Uri)
  (:require
   [goog.events :as e]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [secretary.core :as secretary :refer-macros [defroute]]
   [goog.history.EventType :as EventType]

   [jemo-murge-admin-frontend.events.units-get :refer [units_get]]
   [jemo-murge-admin-frontend.events.model-unit-get :refer [model_unit_get]]
   [jemo-murge-admin-frontend.events.second-level-get-config :refer [second_level_get_config]]
   [jemo-murge-admin-frontend.events.third-level-get-config :refer [third_level_get_config]]
   [jemo-murge-admin-frontend.events.second-level-get-common-attributes :refer [second_level_get_common_attributes]]
   [jemo-murge-admin-frontend.events.product-get :refer [product_get]]
   [jemo-murge-admin-frontend.events.banner-get :refer [banner_get]]
   [jemo-murge-admin-frontend.events.first-level-get-config :refer [first_level_get_config]]
   [jemo-murge-admin-frontend.events.orders-history-get :refer [orders_history_get]]
   [jemo-murge-admin-frontend.events.order-get :refer [order_get]]
   [jemo-murge-admin-frontend.events.statuses-get :refer [statuses_get]]
   [jemo-murge-admin-frontend.events.managers-get :refer [managers_get]]
   [jemo-murge-admin-frontend.events.owners-get :refer [owners_get]]
   [jemo-murge-admin-frontend.events.managers-request-get :refer [managers_request_get]]
   [jemo-murge-admin-frontend.events.payment-provider-get :refer [payment_provider_get]]
   [jemo-murge-admin-frontend.events.delivery-provider-get :refer [delivery_provider_get]]
   [jemo-murge-admin-frontend.events.settings-get :refer [settings_get]]
   [reagent.core :as reagent]
   )
   )


(set! *warn-on-infer* true)

(defn hook-browser-navigation! []
  (doto (Html5History.)
    (e/listen
     EventType/NAVIGATE
     (fn [^js/Foo.Bar event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))



(defn routes []

  (secretary/set-config! :prefix "#")

  (defroute "/" []
    (swap! app-state assoc :page :units)
    (first_level_get_config)
    (banner_get {})
    (units_get (assoc {}
                      :actual (case (:products_mode @app-state)
                                "catalog" ["t" "true"]
                                "archive" ["f" "false"])) "") 
    )

  (defroute "/units" []
    (swap! app-state assoc :page :units)
    (first_level_get_config)
    (banner_get {})
    (units_get (assoc {}
                      :actual (case (:products_mode @app-state)
                                "catalog" ["t" "true"]
                                "archive" ["f" "false"])) "")

    )

  (defroute "/model-unit/:unit_id" [unit_id] 
    (let [decoded_unit_id (js/decodeURIComponent unit_id)]
      (swap! app-state assoc :page :model_unit)
      (swap! app-state assoc :current_unit_id decoded_unit_id)
      (second_level_get_config)
      (second_level_get_common_attributes {:unit_id [(:current_unit_id @app-state)]})
      (model_unit_get (assoc {}
                             :unit_id [(:current_unit_id @app-state)]
                             :actual (case (:products_mode @app-state)
                                       "catalog" ["t" "true"]
                                       "archive" ["f" "false"])) "")
      )
    )
  
  (defroute "/new-unit" []
    (let []
      (swap! app-state assoc :page :new_unit)
      (second_level_get_config)
      )
    )
  
  (defroute "/settings" []
    (let []
      (swap! app-state assoc :page :settings) 
      (settings_get)
      )
    )

  (defroute "/product/:product_id" [product_id]
    (let [decoded_product_id (js/decodeURIComponent product_id)]
      (third_level_get_config)
      (swap! app-state assoc :page :product)
      (swap! app-state assoc :product_draft {} :scroll_restore_y nil)
      (swap! app-state assoc :current_product_id decoded_product_id)
      (product_get (assoc {}
                          :product_id [(:current_product_id @app-state)]
                          ) "")
      )
    )
  

  (defroute "/orders-history" []
    (let []
      (swap! app-state assoc :page :orders_history)
      (orders_history_get {})
      (statuses_get)
      (delivery_provider_get)
      (payment_provider_get)
      )
    )

  (defroute "/order/:order_id" [order_id]
    (let [decoded_order_id (js/decodeURIComponent order_id)]
      (swap! app-state assoc :page :order)
      (swap! app-state assoc :current_order_id decoded_order_id)
      (order_get order_id)
      (statuses_get)
      (delivery_provider_get)
      (payment_provider_get)
      )
    )
  
  (defroute "/managers" []
    (let []
      (swap! app-state assoc :page :managers)
      (managers_get)
      (owners_get)
      (managers_request_get)
      )
    )
  
  (defroute "/payment-methods" []
    (let []
      (swap! app-state assoc :page :payment_methods)
      (swap! app-state assoc :current_payment_provider_name nil)
      (payment_provider_get)
      )
    )

  (defroute "/payment-methods/:payment_provider_name" [payment_provider_name]
    (let [decoded_provider (js/decodeURIComponent payment_provider_name)]
      (swap! app-state assoc :page :payment_methods)
      (swap! app-state assoc :current_payment_provider_name decoded_provider)
      (payment_provider_get)
      )
    )

  (defroute "/delivery-methods" []
    (let []
      (swap! app-state assoc :page :delivery_methods)
      (swap! app-state assoc :current_delivery_provider_name nil)
      (delivery_provider_get)
      )
    )

  (defroute "/delivery-methods/:delivery_provider_name" [delivery_provider_name]
    (let [decoded_provider (js/decodeURIComponent delivery_provider_name)]
      (swap! app-state assoc :page :delivery_methods)
      (swap! app-state assoc :current_delivery_provider_name decoded_provider)
      (delivery_provider_get)
      )
    )


  (hook-browser-navigation!))
