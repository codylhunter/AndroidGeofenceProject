from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.csrf import csrf_exempt
from rest_framework.renderers import JSONRenderer
from rest_framework.parsers import JSONParser
from timelog.models import Activity
from timelog.serializers import ActivitySerializer

class JSONResponse(HttpResponse):
    """
    An HttpResponse that renders its content into JSON.
    """
    def __init__(self, data, **kwargs):
        content = JSONRenderer().render(data)
        kwargs['content_type'] = 'application/json'
        super(JSONResponse, self).__init__(content, **kwargs)

@csrf_exempt
def activity_list_by_location(request, key):
    """
    Lists all activities based on the given location
    """
    if request.method == 'GET':
        try:
            activities = Activity.objects.filter(location__iexact = key)
            serializer = ActivitySerializer(activities, many=True)
            return JSONResponse(serializer.data)
        except Activity.DoesNotExist:
            return HttpResponse(status=404)

@csrf_exempt
def activity_list_by_date(request, key):
    """
    Lists all activities based on the given date
    """
    if request.method == 'GET':
        try:
            activities = Activity.objects.filter(date__iexact = key)
            serializer = ActivitySerializer(activities, many=True)
            return JSONResponse(serializer.data)
        except Activity.DoesNotExist:
            return HttpResponse(status=404)

@csrf_exempt
def activity_post(request):
    """
    Post a new activity
    """
    if request.method == 'POST':
        data = JSONParser().parse(request)
        serializer = ActivitySerializer(data=data)
        if serializer.is_valid():
            serializer.save()
            return JSONResponse(serializer.data, status=201)
        else:
            return JSONResponse(serializer.errors, status=400)
