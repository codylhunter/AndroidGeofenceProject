from django.conf.urls import patterns, url

urlpatterns = patterns('timelog.views',
    url(r'^(?i)api/activity/$', 'activity_post'),
    url(r'^(?i)api/activity/date/(?P<key>[0-9-]+)', 'activity_list_by_date'),
    url(r'^(?i)api/activity/location/(?P<key>[a-zA-Z0-9_ -]+)', 'activity_list_by_location'),
)