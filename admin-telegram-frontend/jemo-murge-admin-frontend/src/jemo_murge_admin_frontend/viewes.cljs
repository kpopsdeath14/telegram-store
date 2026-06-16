(ns jemo-murge-admin-frontend.viewes
  (:require
   [reagent.core :as reagent]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.ratom :as ratom]
   [jemo-murge-admin-frontend.pages.units.units :refer [units_page]]
   [jemo-murge-admin-frontend.pages.model-unit.model-unit :refer [model_unit_page]]
   [jemo-murge-admin-frontend.pages.product.product :refer [product_page]]
   [jemo-murge-admin-frontend.pages.new-unit.new-unit :refer [new_unit_page]]
   [jemo-murge-admin-frontend.pages.orders-history.orders-history :refer [orders_history]]
   [jemo-murge-admin-frontend.pages.order.order :refer [order_page]]
   [jemo-murge-admin-frontend.pages.managers.managers :refer [managers_page]]
   [jemo-murge-admin-frontend.pages.settings.settings :refer [settings_page]]
   [jemo-murge-admin-frontend.pages.payment-methods.payment-methods :refer [payment_methods_page]]
   [jemo-murge-admin-frontend.pages.delivery-methods.delivery-methods :refer [delivery_methods_page]]
   )
  )


(defmulti current-page #(@app-state :page))


(defmethod current-page :units []
  [units_page])

(defmethod current-page :model_unit []
  [model_unit_page])

(defmethod current-page :new_unit []
  [new_unit_page]
  )

(defmethod current-page :product []
  [product_page]
  )

(defmethod current-page :orders_history []
  [orders_history]
  )

(defmethod current-page :order []
  [order_page]
  )

(defmethod current-page :managers []
  [managers_page] 
  )

(defmethod current-page :settings []
  [settings_page]
  )

(defmethod current-page :payment_methods []
  [payment_methods_page]
  )

(defmethod current-page :delivery_methods []
  [delivery_methods_page]
  )
