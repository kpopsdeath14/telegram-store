(ns jemo-murge-admin-frontend.api-uri-maker
  (:require
   [jemo-murge-admin-frontend.db :refer [app-state]]
   )
  )

(defn api_uri_maker [route]
  (str (:backend_url (:config @app-state)) route)
  )

(defn image_uri_maker [image_name]
  (str (:frontend_url (:config @app-state)) (:image_path (:config @app-state)) image_name)
  )

(defn banner_uri_maker [image_name]
  (str (:frontend_url (:config @app-state)) (:banner_path (:config @app-state)) image_name)
  )

